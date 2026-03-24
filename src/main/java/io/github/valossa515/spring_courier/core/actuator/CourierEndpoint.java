package io.github.valossa515.spring_courier.core.actuator;

import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.ExceptionHandlerRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.PostProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.PreProcessorRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot Actuator endpoint that exposes Spring Courier internals
 * for monitoring and debugging purposes.
 *
 * <p>Available at {@code /actuator/courier} when the Actuator starter is
 * on the classpath.
 */
@Endpoint(id = "courier")
public class CourierEndpoint {

    private final HandlerRegistry handlerRegistry;
    private final NotificationRegistry notificationRegistry;
    private final PipelineRegistry pipelineRegistry;
    private final PreProcessorRegistry preProcessorRegistry;
    private final PostProcessorRegistry postProcessorRegistry;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;

    public CourierEndpoint(
            HandlerRegistry handlerRegistry,
            NotificationRegistry notificationRegistry,
            PipelineRegistry pipelineRegistry,
            PreProcessorRegistry preProcessorRegistry,
            PostProcessorRegistry postProcessorRegistry,
            ExceptionHandlerRegistry exceptionHandlerRegistry) {
        this.handlerRegistry = handlerRegistry;
        this.notificationRegistry = notificationRegistry;
        this.pipelineRegistry = pipelineRegistry;
        this.preProcessorRegistry = preProcessorRegistry;
        this.postProcessorRegistry = postProcessorRegistry;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
    }

    @ReadOperation
    public Map<String, Object> courierInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // Handlers
        Map<String, Object> handlers = new LinkedHashMap<>();
        handlers.put("count", handlerRegistry.getHandlerCount());
        List<String> handlerTypes = handlerRegistry.getHandlers().entrySet()
                .stream()
                .map(e -> e.getKey().getSimpleName()
                        + " -> " + e.getValue().getClass().getSimpleName())
                .sorted()
                .toList();
        handlers.put("registered", handlerTypes);
        info.put("handlers", handlers);

        // Notifications
        Map<String, Object> notifications = new LinkedHashMap<>();
        notifications.put("handlerCount", notificationRegistry.getHandlerCount());
        info.put("notifications", notifications);

        // Pipeline
        Map<String, Object> pipeline = new LinkedHashMap<>();
        pipeline.put("behaviorCount", pipelineRegistry.getBehaviorCount());
        info.put("pipeline", pipeline);

        // Processors
        Map<String, Object> processors = new LinkedHashMap<>();
        processors.put("preProcessorCount", preProcessorRegistry.getCount());
        processors.put("postProcessorCount", postProcessorRegistry.getCount());
        processors.put("exceptionHandlerCount",
                exceptionHandlerRegistry.getCount());
        info.put("processors", processors);

        // Registry status
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("handlerRegistryFrozen", handlerRegistry.isFrozen());
        status.put("notificationRegistryFrozen",
                notificationRegistry.isFrozen());
        status.put("pipelineRegistryFrozen", pipelineRegistry.isFrozen());
        info.put("status", status);

        return info;
    }
}
