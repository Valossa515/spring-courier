package io.github.valossa515.spring_courier.core.slack;

import io.github.valossa515.spring_courier.config.CourierProperties;
import io.github.valossa515.spring_courier.core.metrics.CourierMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SlackAlertManagerCoverageTest {

    private MeterRegistry meterRegistry;
    private SlackNotifier notifier;
    private CourierProperties.Slack slackConfig;
    private SlackAlertManager manager;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        notifier = mock(SlackNotifier.class);
        when(notifier.sendFiring(
                anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(notifier.sendResolved(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        slackConfig = new CourierProperties.Slack();
        slackConfig.setEvaluationIntervalSeconds(10);
        slackConfig.setCooldownMinutes(1);
        slackConfig.setForDurationSeconds(0);
        slackConfig.setAppName("Test App");
        manager = new SlackAlertManager(
                meterRegistry, notifier, slackConfig);
    }

    private void registerBaseCounters() {
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);
        Counter.builder(CourierMetrics.HANDLER_TIMEOUTS)
                .register(meterRegistry);
        Counter.builder(CourierMetrics.VALIDATION_FAILURES)
                .register(meterRegistry);
    }

    // ── P99 Latency ──────────────────────────────────────────────

    @Test
    void evaluateDetectsHighP99Latency() {
        Timer timer = Timer.builder(CourierMetrics.SEND_DURATION)
                .publishPercentiles(0.99)
                .register(meterRegistry);
        timer.record(Duration.ofSeconds(5));

        registerBaseCounters();

        slackConfig.getThresholds().setP99LatencySeconds(1.0);

        manager.evaluate();
        manager.evaluate();

        verify(notifier).sendFiring(
                eq("high-p99-latency"),
                eq(AlertSeverity.WARNING),
                eq("Courier: High p99 Latency"),
                anyString(),
                eq("Test App"));
    }

    @Test
    void evaluateDoesNotFireP99WhenBelowThreshold() {
        Timer timer = Timer.builder(CourierMetrics.SEND_DURATION)
                .publishPercentiles(0.99)
                .register(meterRegistry);
        timer.record(Duration.ofMillis(50));

        registerBaseCounters();

        slackConfig.getThresholds().setP99LatencySeconds(1.0);

        manager.evaluate();
        manager.evaluate();

        verify(notifier, never()).sendFiring(
                eq("high-p99-latency"), any(),
                anyString(), anyString(), anyString());
    }

    @Test
    void evaluateSkipsP99WhenNoTimerRegistered() {
        registerBaseCounters();

        manager.evaluate();
        manager.evaluate();

        verify(notifier, never()).sendFiring(
                eq("high-p99-latency"), any(),
                anyString(), anyString(), anyString());
    }

    // ── Throughput Drop ──────────────────────────────────────────

    @Test
    void evaluateDetectsThroughputDrop() {
        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        slackConfig.getThresholds().setThroughputDropRatio(0.5);

        successCounter.increment(1000);
        manager.evaluate();

        // No new sends → currentRate = 0 → drop ratio = 1.0
        manager.evaluate();

        verify(notifier).sendFiring(
                eq("throughput-drop"),
                eq(AlertSeverity.CRITICAL),
                eq("Courier: Throughput Drop"),
                anyString(),
                eq("Test App"));
    }

    @Test
    void evaluateDoesNotFireThroughputDropWhenPrevRateZero() {
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        manager.evaluate();
        manager.evaluate();

        verify(notifier, never()).sendFiring(
                eq("throughput-drop"), any(),
                anyString(), anyString(), anyString());
    }

    // ── Error ratio with no activity ─────────────────────────────

    @Test
    void evaluateTransitionsErrorRatioWhenNoActivity() {
        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter errorCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        reset(notifier);

        manager.evaluate();

        verify(notifier).sendResolved(
                eq("high-error-ratio"),
                anyString(),
                eq("Test App"));
    }

    // ── Cooldown logic ───────────────────────────────────────────

    @Test
    void fireCooldownPreventsRepeatedNotifications()
            throws Exception {
        slackConfig.setCooldownMinutes(60);
        slackConfig.setForDurationSeconds(0);

        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter errorCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        verify(notifier).sendFiring(
                eq("high-error-ratio"), any(),
                anyString(), anyString(), anyString());
        reset(notifier);

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        verify(notifier, never()).sendFiring(
                eq("high-error-ratio"), any(),
                anyString(), anyString(), anyString());
    }

    // ── For-duration pending ─────────────────────────────────────

    @Test
    void fireForDurationPreventsImmediateFiring() {
        slackConfig.setForDurationSeconds(300);

        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter errorCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        verify(notifier, never()).sendFiring(
                eq("high-error-ratio"), any(),
                anyString(), anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void fireForDurationFiresAfterPendingExpires()
            throws Exception {
        slackConfig.setForDurationSeconds(1);

        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter errorCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        Field alertStatesField =
                SlackAlertManager.class.getDeclaredField("alertStates");
        alertStatesField.setAccessible(true);
        Map<String, Object> states =
                (Map<String, Object>) alertStatesField.get(manager);
        Object state = states.get("high-error-ratio");
        if (state != null) {
            Field conditionSinceField =
                    state.getClass().getDeclaredField("conditionSince");
            conditionSinceField.setAccessible(true);
            conditionSinceField.set(state,
                    Instant.now().minus(Duration.ofSeconds(10)));
        }

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        verify(notifier).sendFiring(
                eq("high-error-ratio"),
                eq(AlertSeverity.WARNING),
                anyString(), anyString(), eq("Test App"));
    }

    // ── Transition edge cases ────────────────────────────────────

    @Test
    void transitionWithNullStateDoesNothing() {
        registerBaseCounters();

        manager.evaluate();
        manager.evaluate();

        verifyNoInteractions(notifier);
    }

    @Test
    void transitionResetsConditionSinceWhenNotFiring()
            throws Exception {
        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter errorCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        slackConfig.setForDurationSeconds(9999);

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        successCounter.increment(40);
        errorCounter.increment(60);
        manager.evaluate();

        successCounter.increment(99);
        errorCounter.increment(1);
        manager.evaluate();

        verify(notifier, never()).sendFiring(
                eq("high-error-ratio"), any(),
                anyString(), anyString(), anyString());
        verify(notifier, never()).sendResolved(
                eq("high-error-ratio"), anyString(), anyString());
    }

    // ── P99 resolved ─────────────────────────────────────────────

    @Test
    void evaluateResolvesP99LatencyWhenRecovered() {
        Timer timer = Timer.builder(CourierMetrics.SEND_DURATION)
                .publishPercentiles(0.99)
                .register(meterRegistry);
        registerBaseCounters();

        slackConfig.getThresholds().setP99LatencySeconds(0.001);

        timer.record(Duration.ofSeconds(5));
        manager.evaluate();

        timer.record(Duration.ofSeconds(5));
        manager.evaluate();

        reset(notifier);

        for (int i = 0; i < 1000; i++) {
            timer.record(Duration.ofNanos(1));
        }
        manager.evaluate();

        verify(notifier).sendResolved(
                eq("high-p99-latency"),
                anyString(),
                eq("Test App"));
    }

    // ── Throughput drop resolved ─────────────────────────────────

    @Test
    void evaluateResolvesThroughputDropWhenRecovered() {
        Counter successCounter = Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_ERROR)
                .register(meterRegistry);

        slackConfig.getThresholds().setThroughputDropRatio(0.5);

        successCounter.increment(1000);
        manager.evaluate();

        // No new sends → triggers throughput drop
        manager.evaluate();

        reset(notifier);

        // Recovery: big burst of sends
        successCounter.increment(100000);
        manager.evaluate();

        verify(notifier).sendResolved(
                eq("throughput-drop"),
                anyString(),
                eq("Test App"));
    }

    // ── Evaluate exception handling ──────────────────────────────

    @Test
    void evaluateHandlesExceptionGracefully() {
        MeterRegistry failingRegistry = mock(MeterRegistry.class);
        when(failingRegistry.find(anyString()))
                .thenThrow(new RuntimeException("registry error"));

        SlackAlertManager failingManager = new SlackAlertManager(
                failingRegistry, notifier, slackConfig);

        assertDoesNotThrow(() -> failingManager.evaluate());
        verifyNoInteractions(notifier);
    }

    // ── Timeout resolved ─────────────────────────────────────────

    @Test
    void evaluateResolvesHandlerTimeoutsWhenRecovered() {
        Counter timeoutCounter = Counter.builder(
                CourierMetrics.HANDLER_TIMEOUTS)
                .register(meterRegistry);
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);

        timeoutCounter.increment(5);
        manager.evaluate();

        timeoutCounter.increment(5);
        manager.evaluate();

        reset(notifier);

        manager.evaluate();

        verify(notifier).sendResolved(
                eq("handler-timeouts"),
                anyString(),
                eq("Test App"));
    }

    // ── Validation spike resolved ────────────────────────────────

    @Test
    void evaluateResolvesValidationSpikeWhenRecovered() {
        Counter validationCounter = Counter.builder(
                CourierMetrics.VALIDATION_FAILURES)
                .register(meterRegistry);
        Counter.builder(CourierMetrics.SEND_TOTAL)
                .tag(CourierMetrics.TAG_OUTCOME,
                        CourierMetrics.OUTCOME_SUCCESS)
                .register(meterRegistry);

        validationCounter.increment(1000);
        manager.evaluate();

        validationCounter.increment(1000);
        manager.evaluate();

        reset(notifier);

        manager.evaluate();

        verify(notifier).sendResolved(
                eq("validation-spike"),
                anyString(),
                eq("Test App"));
    }

    // ── MetricSnapshot record accessors ──────────────────────────

    @Test
    void metricSnapshotRecordFields() {
        Instant now = Instant.now();
        SlackAlertManager.MetricSnapshot snapshot =
                new SlackAlertManager.MetricSnapshot(
                        100.0, 5.0, 2.0, 3.0, 0.5, now);

        assertEquals(100.0, snapshot.sendTotal());
        assertEquals(5.0, snapshot.sendErrors());
        assertEquals(2.0, snapshot.timeouts());
        assertEquals(3.0, snapshot.validationFailures());
        assertEquals(0.5, snapshot.p99LatencySeconds());
        assertEquals(now, snapshot.timestamp());
        assertTrue(snapshot.toString().contains("100.0"));
    }

    // ── Stop lifecycle edge case ─────────────────────────────────

    @Test
    void stopWithoutStartDoesNotThrow() {
        SlackAlertManager freshManager = new SlackAlertManager(
                meterRegistry, notifier, slackConfig);
        assertDoesNotThrow(freshManager::stop);
    }
}
