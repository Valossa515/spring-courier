package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.CourierMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CourierMetricsAutoConfigurationTest {

    @Test
    void createsCourierMetricsBean() {
        CourierMetricsAutoConfiguration configuration =
                new CourierMetricsAutoConfiguration();
        MeterRegistry registry = new SimpleMeterRegistry();

        CourierMetrics courierMetrics =
                configuration.courierMetrics(registry);

        assertNotNull(courierMetrics);
        assertNotNull(courierMetrics.getRegistry());
    }
}
