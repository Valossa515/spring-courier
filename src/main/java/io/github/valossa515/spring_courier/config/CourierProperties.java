package io.github.valossa515.spring_courier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for Spring Courier.
 *
 * <p>Properties can be set via {@code application.properties} / {@code application.yml}
 * using the prefix {@code spring.courier}.
 *
 * <pre>
 *   spring.courier.async-timeout-ms=60000
 * </pre>
 */
@ConfigurationProperties(prefix = "spring.courier")
public class CourierProperties {

    private static final long MIN_TIMEOUT_MS = 100;
    private static final long MAX_TIMEOUT_MS = 600_000;

    /**
     * Timeout in milliseconds for asynchronous handler execution.
     * Defaults to 30 000 ms (30 seconds).
     * Must be between {@value #MIN_TIMEOUT_MS} and {@value #MAX_TIMEOUT_MS}.
     */
    private long asyncTimeoutMs = 30_000;

    /**
     * Logging behavior configuration group.
     */
    private Logging logging = new Logging();

    /**
     * Metrics configuration group.
     */
    private Metrics metrics = new Metrics();

    /**
     * Notification publishing strategy configuration group.
     */
    private Notifications notifications = new Notifications();

    /**
     * Slack alerting configuration group.
     */
    private Slack slack = new Slack();

    public long getAsyncTimeoutMs() {
        return asyncTimeoutMs;
    }

    public void setAsyncTimeoutMs(long asyncTimeoutMs) {
        if (asyncTimeoutMs < MIN_TIMEOUT_MS || asyncTimeoutMs > MAX_TIMEOUT_MS) {
            throw new IllegalArgumentException(
                    "asyncTimeoutMs must be between " + MIN_TIMEOUT_MS + " and " + MAX_TIMEOUT_MS
                            + ", got: " + asyncTimeoutMs);
        }
        this.asyncTimeoutMs = asyncTimeoutMs;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    public Slack getSlack() {
        return slack;
    }

    public void setSlack(Slack slack) {
        this.slack = slack;
    }

    /**
     * Logging behavior sub-properties (prefix {@code spring.courier.logging}).
     */
    public static class Logging {

        /**
         * Whether the built-in {@code LoggingBehavior} pipeline is enabled.
         * Defaults to {@code false}.
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Notification publishing sub-properties (prefix {@code spring.courier.notifications}).
     */
    public static class Notifications {

        /**
         * Strategy used when publishing notifications to multiple handlers.
         * Defaults to {@code SEQUENTIAL}.
         */
        private PublishStrategy publishStrategy = PublishStrategy.SEQUENTIAL;

        public PublishStrategy getPublishStrategy() {
            return publishStrategy;
        }

        public void setPublishStrategy(PublishStrategy publishStrategy) {
            this.publishStrategy = publishStrategy;
        }
    }

    /**
     * Notification publishing strategies.
     */
    public enum PublishStrategy {

        /** Handlers are invoked one by one in registration order. */
        SEQUENTIAL,

        /** Handlers are invoked in parallel; waits for all to complete. */
        PARALLEL_WHEN_ALL,

        /**
         * Handlers are invoked sequentially but execution stops
         * immediately when the first handler throws an exception.
         */
        STOP_ON_FIRST_ERROR
    }

    /**
     * Metrics sub-properties (prefix {@code spring.courier.metrics}).
     */
    public static class Metrics {

        /**
         * Whether Micrometer metrics instrumentation is enabled.
         * Defaults to {@code true} when {@code micrometer-core} is on the classpath.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Slack alerting sub-properties (prefix {@code spring.courier.slack}).
     */
    public static class Slack {

        private static final int MIN_INTERVAL = 10;
        private static final int MAX_INTERVAL = 3600;
        private static final int MIN_COOLDOWN = 1;
        private static final int MAX_COOLDOWN = 1440;

        /**
         * Slack Incoming Webhook URL. When set, enables Slack alerting.
         */
        private String webhookUrl;

        /**
         * Slack channel override (e.g. {@code #courier-alerts}).
         */
        private String channel;

        /**
         * Display name for the application in alert messages.
         * Defaults to {@code "Spring Courier"}.
         */
        private String appName = "Spring Courier";

        /**
         * Whether Slack alerting is enabled.
         * Defaults to {@code true}; requires {@code webhook-url} to be set.
         */
        private boolean enabled = true;

        /**
         * How often (in seconds) alert rules are evaluated.
         * Defaults to 60. Must be between 10 and 3600.
         */
        private int evaluationIntervalSeconds = 60;

        /**
         * Minimum minutes between repeated alerts for the same rule.
         * Defaults to 15. Must be between 1 and 1440.
         */
        private int cooldownMinutes = 15;

        /**
         * How long (in seconds) a condition must hold before firing.
         * Defaults to 300 (5 minutes).
         */
        private int forDurationSeconds = 300;

        /**
         * Alert threshold configuration.
         */
        private Thresholds thresholds = new Thresholds();

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getEvaluationIntervalSeconds() {
            return evaluationIntervalSeconds;
        }

        public void setEvaluationIntervalSeconds(int evaluationIntervalSeconds) {
            if (evaluationIntervalSeconds < MIN_INTERVAL
                    || evaluationIntervalSeconds > MAX_INTERVAL) {
                throw new IllegalArgumentException(
                        "evaluationIntervalSeconds must be between "
                                + MIN_INTERVAL + " and " + MAX_INTERVAL);
            }
            this.evaluationIntervalSeconds = evaluationIntervalSeconds;
        }

        public int getCooldownMinutes() {
            return cooldownMinutes;
        }

        public void setCooldownMinutes(int cooldownMinutes) {
            if (cooldownMinutes < MIN_COOLDOWN
                    || cooldownMinutes > MAX_COOLDOWN) {
                throw new IllegalArgumentException(
                        "cooldownMinutes must be between "
                                + MIN_COOLDOWN + " and " + MAX_COOLDOWN);
            }
            this.cooldownMinutes = cooldownMinutes;
        }

        public int getForDurationSeconds() {
            return forDurationSeconds;
        }

        public void setForDurationSeconds(int forDurationSeconds) {
            this.forDurationSeconds = forDurationSeconds;
        }

        public Thresholds getThresholds() {
            return thresholds;
        }

        public void setThresholds(Thresholds thresholds) {
            this.thresholds = thresholds;
        }

        /**
         * Alert threshold values (prefix {@code spring.courier.slack.thresholds}).
         */
        public static class Thresholds {

            /** Error ratio threshold (0.0–1.0). Default 0.05 (5%). */
            private double errorRatio = 0.05;

            /** p99 latency threshold in seconds. Default 1.0s. */
            private double p99LatencySeconds = 1.0;

            /** Validation failures per second threshold. Default 10.0. */
            private double validationRate = 10.0;

            /** Throughput drop ratio (0.0–1.0). Default 0.5 (50% drop). */
            private double throughputDropRatio = 0.5;

            public double getErrorRatio() {
                return errorRatio;
            }

            public void setErrorRatio(double errorRatio) {
                this.errorRatio = errorRatio;
            }

            public double getP99LatencySeconds() {
                return p99LatencySeconds;
            }

            public void setP99LatencySeconds(double p99LatencySeconds) {
                this.p99LatencySeconds = p99LatencySeconds;
            }

            public double getValidationRate() {
                return validationRate;
            }

            public void setValidationRate(double validationRate) {
                this.validationRate = validationRate;
            }

            public double getThroughputDropRatio() {
                return throughputDropRatio;
            }

            public void setThroughputDropRatio(double throughputDropRatio) {
                this.throughputDropRatio = throughputDropRatio;
            }
        }
    }
}
