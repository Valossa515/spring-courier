package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.metrics.MeteredCourier;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.ExceptionHandlerRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.PostProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.PreProcessorRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

/**
 * Auto-configuration that replaces the default {@link Courier} bean with a
 * {@link MeteredCourier} when Micrometer is on the classpath.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "spring.courier.metrics.enabled",
        havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure"
        + ".metrics.CompositeMeterRegistryAutoConfiguration")
@AutoConfigureBefore(CourierAutoConfiguration.class)
public class CourierMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Courier.class)
    public MeteredCourier meteredCourier(
            HandlerRegistry handlerRegistry,
            NotificationRegistry notificationRegistry,
            PipelineExecutor pipelineExecutor,
            PipelineRegistry pipelineRegistry,
            CourierProperties properties,
            ObjectProvider<Executor> asyncExecutor,
            MeterRegistry meterRegistry,
            PreProcessorRegistry preProcessorRegistry,
            PostProcessorRegistry postProcessorRegistry,
            ExceptionHandlerRegistry exceptionHandlerRegistry) {
        return new MeteredCourier(
                handlerRegistry, notificationRegistry,
                pipelineExecutor, pipelineRegistry,
                asyncExecutor.getIfUnique(),
                properties.getAsyncTimeoutMs(), meterRegistry,
                preProcessorRegistry, postProcessorRegistry,
                exceptionHandlerRegistry,
                properties.getNotifications().getPublishStrategy());
    }
}
