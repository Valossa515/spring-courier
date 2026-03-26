package io.github.valossa515.spring_courier.core.pipelines;

/**
 * Abstraction for recording metrics inside pipeline behaviors
 * (cache, retry, idempotency) without coupling to Micrometer.
 *
 * <p>When Micrometer is on the classpath, a real implementation is
 * wired automatically. Otherwise, the {@link #NOOP} instance is used.
 */
public interface BehaviorMetrics {

    /**
     * Increments a named counter, tagged with the request type.
     *
     * @param metricName  dot-notation metric name
     *                    (e.g. {@code courier.cache.hits})
     * @param requestType simple class name of the request
     */
    void incrementCounter(String metricName,
            String requestType);

    /**
     * No-op implementation used when no metrics library is
     * available.
     */
    BehaviorMetrics NOOP = (metricName, requestType) -> { };
}
