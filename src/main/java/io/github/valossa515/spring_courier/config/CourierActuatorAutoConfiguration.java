package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.actuator.CourierEndpoint;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.ExceptionHandlerRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.PostProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.PreProcessorRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that registers the {@code /actuator/courier} endpoint
 * when Spring Boot Actuator is on the classpath.
 */
@Configuration
@ConditionalOnClass(Endpoint.class)
@AutoConfigureAfter(CourierAutoConfiguration.class)
public class CourierActuatorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CourierEndpoint courierEndpoint(
            HandlerRegistry handlerRegistry,
            NotificationRegistry notificationRegistry,
            PipelineRegistry pipelineRegistry,
            PreProcessorRegistry preProcessorRegistry,
            PostProcessorRegistry postProcessorRegistry,
            ExceptionHandlerRegistry exceptionHandlerRegistry) {
        return new CourierEndpoint(
                handlerRegistry, notificationRegistry,
                pipelineRegistry, preProcessorRegistry,
                postProcessorRegistry, exceptionHandlerRegistry);
    }
}
