package io.github.valossa515.spring_courier.core.slack;

import io.github.valossa515.spring_courier.config.CourierProperties;
import io.github.valossa515.spring_courier.core.metrics.CourierMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SlackAlertManagerTest {

    private MeterRegistry meterRegistry;
    private SlackNotifier notifier;
    private CourierProperties.Slack slackConfig;
    private SlackAlertManager manager;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        notifier = mock(SlackNotifier.class);
        slackConfig = new CourierProperties.Slack();
        slackConfig.setEvaluationIntervalSeconds(10);
        slackConfig.setCooldownMinutes(1);
        slackConfig.setForDurationSeconds(0);
        slackConfig.setAppName("Test App");
        manager = new SlackAlertManager(meterRegistry, notifier, slackConfig);
    }

    @Test
    void takeSnapshotReadsMetrics() {
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry).increment(100);
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry).increment(5);
        Counter.builder(CourierMetrics.HANDLER_TIMEOUTS)
                .register(meterRegistry).increment(2);
        Counter.builder(CourierMetrics.VALIDATION_FAILURES)
                .register(meterRegistry).increment(3);

        SlackAlertManager.MetricSnapshot snapshot = manager.takeSnapshot();

        assertEquals(105.0, snapshot.sendTotal(), 0.01);
        assertEquals(5.0, snapshot.sendErrors(), 0.01);
        assertEquals(2.0, snapshot.timeouts(), 0.01);
        assertEquals(3.0, snapshot.validationFailures(), 0.01);
        assertNotNull(snapshot.timestamp());
    }

    @Test
    void takeSnapshotHandlesEmptyRegistry() {
        SlackAlertManager.MetricSnapshot snapshot = manager.takeSnapshot();

        assertEquals(0.0, snapshot.sendTotal(), 0.01);
        assertEquals(0.0, snapshot.sendErrors(), 0.01);
        assertEquals(0.0, snapshot.timeouts(), 0.01);
        assertEquals(0.0, snapshot.validationFailures(), 0.01);
        assertEquals(-1.0, snapshot.p99LatencySeconds(), 0.01);
    }

    @Test
    void evaluateSkipsFirstCycleBecauseNoPrevious() {
        manager.evaluate();
        verifyNoInteractions(notifier);
    }

    @Test
    void evaluateDetectsHighErrorRatio() {
        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter errorCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        successCounter.increment(90);
        errorCounter.increment(10);
        manager.evaluate();

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        verify(notifier).sendFiring(
                eq("high-error-ratio"),
                eq(AlertSeverity.WARNING),
                eq("Courier: High Error Ratio"),
                contains("failing"),
                eq("Test App"));
    }

    @Test
    void evaluateDetectsHandlerTimeouts() {
        Counter timeoutCounter = Counter.builder(CourierMetrics.HANDLER_TIMEOUTS)
                .register(meterRegistry);

        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);

        manager.evaluate();

        timeoutCounter.increment(5);
        manager.evaluate();

        verify(notifier).sendFiring(
                eq("handler-timeouts"),
                eq(AlertSeverity.CRITICAL),
                eq("Courier: Handler Timeouts Detected"),
                anyString(),
                eq("Test App"));
    }

    @Test
    void evaluateDoesNotFireBelowThreshold() {
        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter errorCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        successCounter.increment(99);
        errorCounter.increment(1);
        manager.evaluate();

        successCounter.increment(99);
        errorCounter.increment(1);
        manager.evaluate();

        verify(notifier, never()).sendFiring(
                eq("high-error-ratio"), any(), anyString(),
                anyString(), anyString());
    }

    @Test
    void evaluateDetectsValidationSpike() {
        Counter validationCounter = Counter.builder(
                CourierMetrics.VALIDATION_FAILURES)
                .register(meterRegistry);

        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);

        manager.evaluate();

        validationCounter.increment(1000);
        manager.evaluate();

        verify(notifier).sendFiring(
                eq("validation-spike"),
                eq(AlertSeverity.WARNING),
                eq("Courier: Validation Spike"),
                anyString(),
                eq("Test App"));
    }

    @Test
    void startAndStopDoNotThrow() {
        assertDoesNotThrow(() -> {
            manager.start();
            manager.stop();
        });
    }

    @Test
    void evaluateSendsResolvedAfterRecovery() {
        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter errorCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        reset(notifier);

        successCounter.increment(99);
        errorCounter.increment(1);
        manager.evaluate();

        verify(notifier).sendResolved(
                eq("high-error-ratio"),
                anyString(),
                eq("Test App"));
    }
}
