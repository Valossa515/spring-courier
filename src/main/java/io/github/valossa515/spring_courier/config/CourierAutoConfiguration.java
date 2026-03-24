package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.ResponseEntityConverter;
import io.github.valossa515.spring_courier.core.pipelines.LoggingBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.BehaviorDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.DefaultResponseEntityConverter;
import io.github.valossa515.spring_courier.core.support.ExceptionHandlerRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.PostProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.PreProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.ProcessorDiscoveryPostProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import java.util.concurrent.Executor;

/**
 * Autoconfiguration that wires the core Spring Courier beans required for the
 * CQRS pipeline and notification support. All beans are registered conditionally
 * so users can provide their own implementations.
 */
@Configuration
@EnableConfigurationProperties(CourierProperties.class)
public class CourierAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static HandlerRegistry handlerRegistry() {
        return new HandlerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static NotificationRegistry notificationRegistry() {
        return new NotificationRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static PipelineRegistry pipelineRegistry() {
        return new PipelineRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static PreProcessorRegistry preProcessorRegistry() {
        return new PreProcessorRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static PostProcessorRegistry postProcessorRegistry() {
        return new PostProcessorRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static ExceptionHandlerRegistry exceptionHandlerRegistry() {
        return new ExceptionHandlerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PipelineExecutor pipelineExecutor(PipelineRegistry pipelineRegistry) {
        return new PipelineExecutor(pipelineRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public Courier courier(HandlerRegistry handlerRegistry,
                           NotificationRegistry notificationRegistry,
                           PipelineExecutor pipelineExecutor,
                           CourierProperties properties,
                           ObjectProvider<Executor> asyncExecutor,
                           PreProcessorRegistry preProcessorRegistry,
                           PostProcessorRegistry postProcessorRegistry,
                           ExceptionHandlerRegistry exceptionHandlerRegistry) {
        return new Courier(handlerRegistry, notificationRegistry,
                pipelineExecutor, asyncExecutor.getIfUnique(),
                properties.getAsyncTimeoutMs(),
                preProcessorRegistry, postProcessorRegistry,
                exceptionHandlerRegistry,
                properties.getNotifications().getPublishStrategy());
    }

    @Bean
    @ConditionalOnMissingBean
    public ResponseEntityConverter responseEntityConverter() {
        return new DefaultResponseEntityConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public static HandlerDiscoveryPostProcessor handlerDiscoveryPostProcessor(
            HandlerRegistry handlerRegistry,
            ApplicationContext applicationContext) {
        return new HandlerDiscoveryPostProcessor(
                handlerRegistry, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public static NotificationDiscoveryPostProcessor notificationDiscoveryPostProcessor(
            NotificationRegistry notificationRegistry,
            ApplicationContext applicationContext) {
        return new NotificationDiscoveryPostProcessor(
                notificationRegistry, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public static BehaviorDiscoveryPostProcessor behaviorDiscoveryPostProcessor(
            PipelineRegistry pipelineRegistry) {
        return new BehaviorDiscoveryPostProcessor(pipelineRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public static ProcessorDiscoveryPostProcessor processorDiscoveryPostProcessor(
            PreProcessorRegistry preProcessorRegistry,
            PostProcessorRegistry postProcessorRegistry,
            ExceptionHandlerRegistry exceptionHandlerRegistry) {
        return new ProcessorDiscoveryPostProcessor(
                preProcessorRegistry, postProcessorRegistry,
                exceptionHandlerRegistry);
    }

    /**
     * Registers the built-in {@link LoggingBehavior} when
     * {@code spring.courier.logging.enabled=true}.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.courier.logging.enabled",
            havingValue = "true")
    @ConditionalOnMissingBean(LoggingBehavior.class)
    public LoggingBehavior loggingBehavior() {
        return new LoggingBehavior();
    }

    /**
     * Freezes all registries after all singleton beans have been initialized,
     * preventing runtime handler/behavior replacement (registry poisoning).
     */
    @Bean
    public SmartInitializingSingleton freezeRegistries(
            HandlerRegistry handlerRegistry,
            NotificationRegistry notificationRegistry,
            PipelineRegistry pipelineRegistry,
            PreProcessorRegistry preProcessorRegistry,
            PostProcessorRegistry postProcessorRegistry,
            ExceptionHandlerRegistry exceptionHandlerRegistry) {
        return () -> {
            handlerRegistry.freeze();
            notificationRegistry.freeze();
            pipelineRegistry.freeze();
            preProcessorRegistry.freeze();
            postProcessorRegistry.freeze();
            exceptionHandlerRegistry.freeze();
        };
    }
}
