package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.metrics.MeteredCourier;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourierMetricsAutoConfigurationTest {

    @Test
    void createsMeteredCourierBean() {
        CourierMetricsAutoConfiguration config = new CourierMetricsAutoConfiguration();
        CourierProperties properties = new CourierProperties();

        HandlerRegistry handlerRegistry = new HandlerRegistry();
        NotificationRegistry notificationRegistry = new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        PipelineExecutor pipelineExecutor = new PipelineExecutor(pipelineRegistry);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        @SuppressWarnings("unchecked")
        ObjectProvider<Executor> executorProvider = mock(ObjectProvider.class);
        when(executorProvider.getIfUnique()).thenReturn(null);

        MeteredCourier courier = config.meteredCourier(
                handlerRegistry, notificationRegistry,
                pipelineExecutor, pipelineRegistry,
                processorRegistry,
                properties, executorProvider, meterRegistry);

        assertNotNull(courier);
        assertInstanceOf(MeteredCourier.class, courier);
    }

    @Test
    void metricsPropertyDefaultsToTrue() {
        CourierProperties properties = new CourierProperties();
        assertTrue(properties.getMetrics().isEnabled());
    }

    @Test
    void metricsPropertyCanBeDisabled() {
        CourierProperties properties = new CourierProperties();
        properties.getMetrics().setEnabled(false);
        assertFalse(properties.getMetrics().isEnabled());
    }

}
