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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Autoconfiguration that wires the core Spring Courier beans required for the
 * CQRS pipeline and notification support.
 */
@Configuration
public class CourierAutoConfiguration {

    @Bean
    public HandlerRegistry handlerRegistry() {
        return new HandlerRegistry();
    }

    @Bean
    public NotificationRegistry notificationRegistry() {
        return new NotificationRegistry();
    }

    @Bean
    public PipelineRegistry pipelineRegistry() {
        return new PipelineRegistry();
    }

    @Bean
    public PipelineExecutor pipelineExecutor(PipelineRegistry pipelineRegistry) {
        return new PipelineExecutor(pipelineRegistry);
    }

    @Bean
    public Courier courier(HandlerRegistry handlerRegistry,
                           NotificationRegistry notificationRegistry,
                           PipelineExecutor pipelineExecutor,
                           ObjectProvider<Executor> executorProvider) {
        Executor asyncExecutor = executorProvider.getIfAvailable(ForkJoinPool::commonPool);
        return new Courier(handlerRegistry, notificationRegistry, pipelineExecutor, asyncExecutor);
    }

    @Bean
    public HandlerDiscoveryPostProcessor handlerDiscoveryPostProcessor(HandlerRegistry handlerRegistry,
                                                                       ApplicationContext applicationContext) {
        return new HandlerDiscoveryPostProcessor(handlerRegistry, applicationContext);
    }

    @Bean
    public NotificationDiscoveryPostProcessor notificationDiscoveryPostProcessor(
            NotificationRegistry notificationRegistry,
            ApplicationContext applicationContext) {
        return new NotificationDiscoveryPostProcessor(notificationRegistry, applicationContext);
    }

    @Bean
    public BehaviorDiscoveryPostProcessor behaviorDiscoveryPostProcessor(PipelineRegistry pipelineRegistry) {
        return new BehaviorDiscoveryPostProcessor(pipelineRegistry);
    }
}
