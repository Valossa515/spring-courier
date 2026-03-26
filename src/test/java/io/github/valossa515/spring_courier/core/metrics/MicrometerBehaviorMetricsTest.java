package io.github.valossa515.spring_courier.core.metrics;

import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MicrometerBehaviorMetricsTest {

    @Test
    void incrementCounterRegistersAndIncrementsMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BehaviorMetrics metrics =
                new MicrometerBehaviorMetrics(registry);

        metrics.incrementCounter("courier.cache.hits",
                "GetUserQuery");
        metrics.incrementCounter("courier.cache.hits",
                "GetUserQuery");

        Counter counter = registry.find("courier.cache.hits")
                .tag("request.type", "GetUserQuery")
                .counter();

        assertNotNull(counter);
        assertEquals(2.0, counter.count());
    }

    @Test
    void differentRequestTypesProduceSeparateCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BehaviorMetrics metrics =
                new MicrometerBehaviorMetrics(registry);

        metrics.incrementCounter("courier.retry.attempts",
                "CmdA");
        metrics.incrementCounter("courier.retry.attempts",
                "CmdB");
        metrics.incrementCounter("courier.retry.attempts",
                "CmdB");

        assertEquals(1.0,
                registry.find("courier.retry.attempts")
                        .tag("request.type", "CmdA")
                        .counter().count());
        assertEquals(2.0,
                registry.find("courier.retry.attempts")
                        .tag("request.type", "CmdB")
                        .counter().count());
    }

    @Test
    void noopDoesNotThrow() {
        BehaviorMetrics.NOOP.incrementCounter(
                "any.metric", "AnyType");
    }
}
