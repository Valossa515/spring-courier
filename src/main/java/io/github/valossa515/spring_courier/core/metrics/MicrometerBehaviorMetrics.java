package io.github.valossa515.spring_courier.core.metrics;

import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;

/**
 * Micrometer-backed {@link BehaviorMetrics} implementation.
 *
 * <p>Registered automatically by
 * {@code CourierMetricsAutoConfiguration} when Micrometer is on the
 * classpath.
 */
public class MicrometerBehaviorMetrics implements BehaviorMetrics {

    private static final Map<String, String> DESCRIPTIONS =
            Map.of(
                    CourierMetrics.CACHE_HITS,
                    "Number of cache hits in CachingBehavior",
                    CourierMetrics.CACHE_MISSES,
                    "Number of cache misses in CachingBehavior",
                    CourierMetrics.RETRY_ATTEMPTS,
                    "Number of retry attempts triggered",
                    CourierMetrics.RETRY_EXHAUSTED,
                    "Number of retries exhausted",
                    CourierMetrics.IDEMPOTENCY_HITS,
                    "Idempotent requests deduplicated",
                    CourierMetrics.IDEMPOTENCY_MISSES,
                    "Idempotent requests executed");

    private final MeterRegistry registry;

    public MicrometerBehaviorMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void incrementCounter(String metricName,
            String requestType) {
        Counter.builder(metricName)
                .description(DESCRIPTIONS.getOrDefault(
                        metricName, metricName))
                .tag(CourierMetrics.TAG_REQUEST_TYPE,
                        requestType)
                .register(registry)
                .increment();
    }
}
