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
     * Metrics configuration group.
     */
    private Metrics metrics = new Metrics();

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

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Whether Micrometer metrics instrumentation is enabled.
     *
     * @return {@code true} if metrics are enabled
     * @deprecated Use {@link #getMetrics()}.{@link Metrics#isEnabled() isEnabled()} instead.
     */
    @Deprecated(since = "1.4.0", forRemoval = true)
    public boolean isMetricsEnabled() {
        return metrics.isEnabled();
    }

    /**
     * @deprecated Use {@link #getMetrics()}.{@link Metrics#setEnabled(boolean) setEnabled(boolean)} instead.
     */
    @Deprecated(since = "1.4.0", forRemoval = true)
    public void setMetricsEnabled(boolean metricsEnabled) {
        metrics.setEnabled(metricsEnabled);
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
}
