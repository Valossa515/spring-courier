package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.BehaviorDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that wires the core Spring Courier beans required for the
 * CQRS pipeline.
 */
@Configuration
public class CourierAutoConfiguration{

    @Bean
    public HandlerRegistry handlerRegistry() {
        return new HandlerRegistry();
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
    public Courier courier(HandlerRegistry handlerRegistry, PipelineExecutor pipelineExecutor) {
        return new Courier(handlerRegistry, pipelineExecutor);
    }

    @Bean
    public HandlerDiscoveryPostProcessor handlerDiscoveryPostProcessor(
            HandlerRegistry handlerRegistry, ApplicationContext applicationContext) {
        return new HandlerDiscoveryPostProcessor(handlerRegistry, applicationContext);
    }

    @Bean
    public BehaviorDiscoveryPostProcessor behaviorDiscoveryPostProcessor(
            PipelineRegistry pipelineRegistry) {
        return new BehaviorDiscoveryPostProcessor(pipelineRegistry);
    }
}
