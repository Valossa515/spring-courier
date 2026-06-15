package io.github.valossa515.spring_courier.core.slack;

import io.github.valossa515.spring_courier.config.CourierProperties;
import io.github.valossa515.spring_courier.core.metrics.CourierMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Periodically evaluates built-in alert rules against Courier metrics and
 * sends Slack notifications when conditions fire or resolve.
 *
 * <p>Each alert rule tracks three timestamps:
 * <ul>
 *   <li><b>conditionSince</b> — when the condition first became true (for "for" duration)</li>
 *   <li><b>firedAt</b> — when FIRING was first notified</li>
 *   <li><b>lastNotifiedAt</b> — last Slack message sent (for cooldown)</li>
 * </ul>
 *
 * <p>Alert lifecycle: {@code OK → PENDING → FIRING → RESOLVED → OK}
 */
public class SlackAlertManager {

    private static final Logger logger = LoggerFactory.getLogger(SlackAlertManager.class);
    private static final double P99_PERCENTILE = 0.99;

    private final MeterRegistry meterRegistry;
    private final SlackNotifier notifier;
    private final CourierProperties.Slack config;
    private final ScheduledExecutorService scheduler;
    private final Map<String, AlertState> alertStates = new ConcurrentHashMap<>();

    private final AtomicReference<SnapshotWindow> snapshotWindow =
            new AtomicReference<>(new SnapshotWindow(null, null));

    public SlackAlertManager(MeterRegistry meterRegistry,
                             SlackNotifier notifier,
                             CourierProperties.Slack config) {
        this.meterRegistry = meterRegistry;
        this.notifier = notifier;
        this.config = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "courier-slack-alert");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts the periodic alert evaluation loop.
     */
    private static final int WARMUP_DELAY_SECONDS = 10;

    public void start() {
        if (!notifier.isConfigured()) {
            logger.warn("Slack alerting disabled — webhook URL is not "
                    + "configured. Set spring.courier.slack.webhook-url "
                    + "to enable.");
            return;
        }

        int interval = config.getEvaluationIntervalSeconds();
        int initialDelay = Math.min(interval, WARMUP_DELAY_SECONDS);
        logger.info("Slack alerting started (interval={}s, cooldown={}min, "
                        + "forDuration={}s)",
                interval, config.getCooldownMinutes(),
                config.getForDurationSeconds());

        scheduler.scheduleAtFixedRate(this::evaluate,
                initialDelay, interval, TimeUnit.SECONDS);
    }

    /**
     * Stops the evaluation loop and releases resources.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Slack alerting stopped");
    }

    void evaluate() {
        try {
            MetricSnapshot current = takeSnapshot();
            SnapshotWindow old = snapshotWindow.getAndUpdate(
                    w -> new SnapshotWindow(current, w.previous()));
            MetricSnapshot prev = old.previous();
            MetricSnapshot prior = old.prior();

            if (prev == null) {
                return;
            }

            double elapsedSeconds = Duration.between(
                    prev.timestamp(), current.timestamp()).toNanos()
                    / 1_000_000_000.0;
            if (elapsedSeconds <= 0) {
                return;
            }

            safeEvaluate("error-ratio",
                    () -> evaluateErrorRatio(current, prev,
                            elapsedSeconds));
            safeEvaluate("p99-latency",
                    () -> evaluateP99Latency(current));
            safeEvaluate("timeouts",
                    () -> evaluateTimeouts(current, prev,
                            elapsedSeconds));
            safeEvaluate("validation-spike",
                    () -> evaluateValidationSpike(current, prev,
                            elapsedSeconds));
            safeEvaluate("throughput-drop",
                    () -> evaluateThroughputDrop(current, prev,
                            prior, elapsedSeconds));

        } catch (Exception e) {
            logger.error("Error during Slack alert evaluation: {}",
                    e.getMessage(), e);
        }
    }

    private void safeEvaluate(String rule, Runnable evaluation) {
        try {
            evaluation.run();
        } catch (Exception e) {
            logger.error("Error evaluating alert rule '{}': {}",
                    rule, e.getMessage(), e);
        }
    }

    private void evaluateErrorRatio(MetricSnapshot current,
                                    MetricSnapshot prev,
                                    double elapsed) {
        double deltaErrors = current.sendErrors() - prev.sendErrors();
        double deltaTotal = current.sendTotal() - prev.sendTotal();

        if (deltaTotal <= 0) {
            transition("high-error-ratio", false);
            return;
        }

        double ratio = deltaErrors / deltaTotal;
        boolean firing = ratio > config.getThresholds().getErrorRatio();

        if (firing) {
            String pct = String.format("%.1f%%", ratio * 100);
            fire("high-error-ratio",
                    AlertSeverity.WARNING,
                    "Courier: High Error Ratio",
                    pct + " of sends are failing (threshold: "
                            + formatPct(config.getThresholds().getErrorRatio())
                            + ").");
        } else {
            transition("high-error-ratio", false);
        }
    }

    private void evaluateP99Latency(MetricSnapshot current) {
        double p99 = current.p99LatencySeconds();
        if (p99 < 0) {
            return;
        }

        double threshold = config.getThresholds().getP99LatencySeconds();
        boolean firing = p99 > threshold;

        if (firing) {
            fire("high-p99-latency",
                    AlertSeverity.WARNING,
                    "Courier: High p99 Latency",
                    String.format("p99 latency is %.3fs (threshold: %.1fs).",
                            p99, threshold));
        } else {
            transition("high-p99-latency", false);
        }
    }

    private void evaluateTimeouts(MetricSnapshot current,
                                  MetricSnapshot prev,
                                  double elapsed) {
        double deltaTimeouts = current.timeouts() - prev.timeouts();
        double rate = deltaTimeouts / elapsed;
        boolean firing = rate > 0;

        if (firing) {
            fire("handler-timeouts",
                    AlertSeverity.CRITICAL,
                    "Courier: Handler Timeouts Detected",
                    String.format("%.1f timeouts/s detected. "
                            + "Check async handlers and "
                            + "spring.courier.async-timeout-ms.", rate));
        } else {
            transition("handler-timeouts", false);
        }
    }

    private void evaluateValidationSpike(MetricSnapshot current,
                                         MetricSnapshot prev,
                                         double elapsed) {
        double delta = current.validationFailures()
                - prev.validationFailures();
        double rate = delta / elapsed;
        double threshold = config.getThresholds().getValidationRate();
        boolean firing = rate > threshold;

        if (firing) {
            fire("validation-spike",
                    AlertSeverity.WARNING,
                    "Courier: Validation Spike",
                    String.format("%.1f validation failures/s "
                            + "(threshold: %.1f/s).", rate, threshold));
        } else {
            transition("validation-spike", false);
        }
    }

    private void evaluateThroughputDrop(MetricSnapshot current,
                                        MetricSnapshot prev,
                                        MetricSnapshot prior,
                                        double elapsed) {
        if (prior == null) {
            transition("throughput-drop", false);
            return;
        }

        double prevElapsed = Duration.between(
                prior.timestamp(), prev.timestamp()).toNanos()
                / 1_000_000_000.0;
        if (prevElapsed <= 0) {
            transition("throughput-drop", false);
            return;
        }

        double prevDelta = prev.sendTotal() - prior.sendTotal();
        double prevRate = prevDelta / prevElapsed;
        double currentDelta = current.sendTotal() - prev.sendTotal();
        double currentRate = currentDelta / elapsed;

        if (prevRate <= 0) {
            transition("throughput-drop", false);
            return;
        }

        double dropRatio = 1.0 - (currentRate / prevRate);
        double threshold = config.getThresholds().getThroughputDropRatio();
        boolean firing = dropRatio > threshold;

        if (firing) {
            fire("throughput-drop",
                    AlertSeverity.CRITICAL,
                    "Courier: Throughput Drop",
                    String.format("Throughput dropped %.0f%% "
                                    + "(threshold: %.0f%%).",
                            dropRatio * 100, threshold * 100));
        } else {
            transition("throughput-drop", false);
        }
    }

    private void fire(String ruleName, AlertSeverity severity,
                      String summary, String detail) {
        AlertState state = alertStates.computeIfAbsent(
                ruleName, k -> new AlertState());
        Instant now = Instant.now();

        if (state.conditionSince == null) {
            state.conditionSince = now;
            logger.debug("Alert condition detected for '{}': waiting "
                    + "forDuration={}s before firing",
                    ruleName, config.getForDurationSeconds());
        }

        long pendingSeconds = Duration.between(
                state.conditionSince, now).getSeconds();
        if (pendingSeconds < config.getForDurationSeconds()) {
            logger.debug("Alert '{}' pending: {}s / {}s",
                    ruleName, pendingSeconds,
                    config.getForDurationSeconds());
            return;
        }

        if (state.lastNotifiedAt != null) {
            long minutesSinceLast = Duration.between(
                    state.lastNotifiedAt, now).toMinutes();
            if (minutesSinceLast < config.getCooldownMinutes()) {
                return;
            }
        }

        state.firing = true;
        state.firedAt = now;
        state.lastNotifiedAt = now;

        logger.warn("Slack alert FIRING: {} — {}", ruleName, detail);
        notifier.sendFiring(ruleName, severity, summary, detail,
                config.getAppName());
    }

    private void transition(String ruleName, boolean conditionMet) {
        AlertState state = alertStates.get(ruleName);
        if (state == null) {
            return;
        }

        if (!conditionMet) {
            if (state.firing) {
                state.firing = false;
                state.conditionSince = null;
                state.firedAt = null;

                logger.info("Slack alert RESOLVED: {}", ruleName);
                notifier.sendResolved(ruleName,
                        "Condition returned to normal.",
                        config.getAppName());
            } else if (state.conditionSince != null) {
                logger.debug("Alert '{}' condition cleared before "
                        + "forDuration was reached — resetting pending timer",
                        ruleName);
                state.conditionSince = null;
            }
        }
    }

    MetricSnapshot takeSnapshot() {
        double sendErrors = sumCounters(CourierMetrics.SEND_TOTAL,
                CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_ERROR);
        double sendSuccess = sumCounters(CourierMetrics.SEND_TOTAL,
                CourierMetrics.TAG_OUTCOME, CourierMetrics.OUTCOME_SUCCESS);
        double sendTotal = sendErrors + sendSuccess;

        double timeouts = sumCounters(CourierMetrics.HANDLER_TIMEOUTS,
                null, null);
        double validationFailures = sumCounters(
                CourierMetrics.VALIDATION_FAILURES, null, null);

        double p99 = readP99Latency();

        return new MetricSnapshot(sendTotal, sendErrors, timeouts,
                validationFailures, p99, Instant.now());
    }

    private double sumCounters(String name, String tagKey, String tagValue) {
        var search = meterRegistry.find(name);
        if (tagKey != null && tagValue != null) {
            search = search.tag(tagKey, tagValue);
        }
        return search.counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    private double readP99Latency() {
        var timers = meterRegistry.find(CourierMetrics.SEND_DURATION)
                .timers();
        double maxP99 = -1.0;
        for (Timer timer : timers) {
            var snapshot = timer.takeSnapshot();
            for (ValueAtPercentile vap : snapshot.percentileValues()) {
                if (Math.abs(vap.percentile() - P99_PERCENTILE) < 0.001) {
                    double value = vap.value(TimeUnit.SECONDS);
                    if (value > maxP99) {
                        maxP99 = value;
                    }
                }
            }
        }
        return maxP99;
    }

    private static String formatPct(double ratio) {
        return String.format("%.0f%%", ratio * 100);
    }

    record MetricSnapshot(double sendTotal,
                          double sendErrors,
                          double timeouts,
                          double validationFailures,
                          double p99LatencySeconds,
                          Instant timestamp) {
    }

    record SnapshotWindow(MetricSnapshot previous,
                          MetricSnapshot prior) {
    }

    private static class AlertState {
        volatile Instant conditionSince;
        volatile Instant firedAt;
        volatile Instant lastNotifiedAt;
        volatile boolean firing;
    }
}
