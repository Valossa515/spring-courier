package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.BehaviorDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public HandlerRegistry handlerRegistry() {
        return new HandlerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationRegistry notificationRegistry() {
        return new NotificationRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PipelineRegistry pipelineRegistry() {
        return new PipelineRegistry();
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
                           ObjectProvider<Executor> asyncExecutor) {
        return new Courier(handlerRegistry, notificationRegistry, pipelineExecutor,
                asyncExecutor.getIfUnique(), properties.getAsyncTimeoutMs());
    }

    @Bean
    @ConditionalOnMissingBean
    public HandlerDiscoveryPostProcessor handlerDiscoveryPostProcessor(HandlerRegistry handlerRegistry,
                                                                       ApplicationContext applicationContext) {
        return new HandlerDiscoveryPostProcessor(handlerRegistry, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationDiscoveryPostProcessor notificationDiscoveryPostProcessor(
            NotificationRegistry notificationRegistry,
            ApplicationContext applicationContext) {
        return new NotificationDiscoveryPostProcessor(notificationRegistry, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public BehaviorDiscoveryPostProcessor behaviorDiscoveryPostProcessor(PipelineRegistry pipelineRegistry) {
        return new BehaviorDiscoveryPostProcessor(pipelineRegistry);
    }

    /**
     * Freezes all registries after all singleton beans have been initialized,
     * preventing runtime handler/behavior replacement (registry poisoning).
     */
    @Bean
    public SmartInitializingSingleton freezeRegistries(
            HandlerRegistry handlerRegistry,
            NotificationRegistry notificationRegistry,
            PipelineRegistry pipelineRegistry) {
        return () -> {
            handlerRegistry.freeze();
            notificationRegistry.freeze();
            pipelineRegistry.freeze();
        };
    }
}
