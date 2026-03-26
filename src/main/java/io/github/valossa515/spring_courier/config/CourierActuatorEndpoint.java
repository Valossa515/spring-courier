package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Actuator endpoint that exposes runtime information about
 * the Courier infrastructure at {@code /actuator/courier}.
 *
 * <p>Provides counts of registered handlers, notification handlers,
 * pipeline behaviors, pre/post processors, and exception handlers,
 * plus the current notification publishing strategy.
 */
@Endpoint(id = "courier")
public class CourierActuatorEndpoint {

    private final HandlerRegistry handlerRegistry;
    private final NotificationRegistry notificationRegistry;
    private final PipelineRegistry pipelineRegistry;
    private final ProcessorRegistry processorRegistry;
    private final CourierProperties properties;

    public CourierActuatorEndpoint(
            HandlerRegistry handlerRegistry,
            NotificationRegistry notificationRegistry,
            PipelineRegistry pipelineRegistry,
            ProcessorRegistry processorRegistry,
            CourierProperties properties) {
        this.handlerRegistry = handlerRegistry;
        this.notificationRegistry = notificationRegistry;
        this.pipelineRegistry = pipelineRegistry;
        this.processorRegistry = processorRegistry;
        this.properties = properties;
    }

    @ReadOperation
    public Map<String, Object> courierInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        Map<String, Object> handlers = new LinkedHashMap<>();
        handlers.put("commands_and_queries",
                handlerRegistry.getHandlerCount());
        handlers.put("notifications",
                notificationRegistry.getHandlerCount());
        info.put("handlers", handlers);

        Map<String, Object> pipeline = new LinkedHashMap<>();
        pipeline.put("behaviors",
                pipelineRegistry.getBehaviorCount());
        pipeline.put("pre_processors",
                processorRegistry.getPreProcessors().size());
        pipeline.put("post_processors",
                processorRegistry.getPostProcessors().size());
        pipeline.put("exception_handlers",
                processorRegistry.getExceptionHandlers().size());
        info.put("pipeline", pipeline);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("async_timeout_ms",
                properties.getAsyncTimeoutMs());
        config.put("notification_strategy",
                properties.getNotificationStrategy().name());
        config.put("logging_enabled",
                properties.getLogging().isEnabled());
        config.put("metrics_enabled",
                properties.getMetrics().isEnabled());
        info.put("config", config);

        Map<String, Object> registries = new LinkedHashMap<>();
        registries.put("handler_registry_frozen",
                handlerRegistry.isFrozen());
        registries.put("notification_registry_frozen",
                notificationRegistry.isFrozen());
        registries.put("pipeline_registry_frozen",
                pipelineRegistry.isFrozen());
        registries.put("processor_registry_frozen",
                processorRegistry.isFrozen());
        info.put("registries", registries);

        return info;
    }
}
