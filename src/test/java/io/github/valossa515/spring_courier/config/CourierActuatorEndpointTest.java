package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierActuatorEndpointTest {

    private CourierActuatorEndpoint endpoint;

    @BeforeEach
    void setUp() {
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        NotificationRegistry notificationRegistry =
                new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        CourierProperties properties = new CourierProperties();

        endpoint = new CourierActuatorEndpoint(
                handlerRegistry, notificationRegistry,
                pipelineRegistry, processorRegistry,
                properties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void courierInfoReturnsExpectedStructure() {
        Map<String, Object> info = endpoint.courierInfo();

        assertNotNull(info);
        assertTrue(info.containsKey("handlers"));
        assertTrue(info.containsKey("pipeline"));
        assertTrue(info.containsKey("config"));
        assertTrue(info.containsKey("registries"));

        Map<String, Object> handlers =
                (Map<String, Object>) info.get("handlers");
        assertEquals(0, handlers.get("commands_and_queries"));
        assertEquals(0, handlers.get("notifications"));

        Map<String, Object> pipeline =
                (Map<String, Object>) info.get("pipeline");
        assertEquals(0, pipeline.get("behaviors"));
        assertEquals(0, pipeline.get("pre_processors"));
        assertEquals(0, pipeline.get("post_processors"));
        assertEquals(0, pipeline.get("exception_handlers"));

        Map<String, Object> config =
                (Map<String, Object>) info.get("config");
        assertEquals(30_000L, config.get("async_timeout_ms"));
        assertEquals("SEQUENTIAL",
                config.get("notification_strategy"));
        assertEquals(true, config.get("logging_enabled"));
        assertEquals(true, config.get("metrics_enabled"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void registriesShowUnfrozenByDefault() {
        Map<String, Object> info = endpoint.courierInfo();
        Map<String, Object> registries =
                (Map<String, Object>) info.get("registries");

        assertEquals(false,
                registries.get("handler_registry_frozen"));
        assertEquals(false,
                registries.get("notification_registry_frozen"));
        assertEquals(false,
                registries.get("pipeline_registry_frozen"));
        assertEquals(false,
                registries.get("processor_registry_frozen"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void configReflectsCustomProperties() {
        CourierProperties props = new CourierProperties();
        props.setAsyncTimeoutMs(60_000);
        props.setNotificationStrategy(
                NotificationPublishingStrategy.PARALLEL_WHEN_ALL);
        props.getLogging().setEnabled(false);

        CourierActuatorEndpoint customEndpoint =
                new CourierActuatorEndpoint(
                        new HandlerRegistry(),
                        new NotificationRegistry(),
                        new PipelineRegistry(),
                        new ProcessorRegistry(),
                        props);

        Map<String, Object> info = customEndpoint.courierInfo();
        Map<String, Object> config =
                (Map<String, Object>) info.get("config");

        assertEquals(60_000L, config.get("async_timeout_ms"));
        assertEquals("PARALLEL_WHEN_ALL",
                config.get("notification_strategy"));
        assertEquals(false, config.get("logging_enabled"));
    }
}
