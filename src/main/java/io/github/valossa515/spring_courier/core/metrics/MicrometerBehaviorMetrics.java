package io.github.valossa515.spring_courier.core.metrics;

import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer-backed {@link BehaviorMetrics} implementation.
 *
 * <p>Registered automatically by
 * {@code CourierMetricsAutoConfiguration} when Micrometer is on the
 * classpath.
 */
public class MicrometerBehaviorMetrics implements BehaviorMetrics {

    private final MeterRegistry registry;

    public MicrometerBehaviorMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void incrementCounter(String metricName,
            String requestType) {
        Counter.builder(metricName)
                .tag(CourierMetrics.TAG_REQUEST_TYPE,
                        requestType)
                .register(registry)
                .increment();
    }
}
