package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.ResponseEntityConverter;
import io.github.valossa515.spring_courier.core.pipelines.CachingBehavior;
import io.github.valossa515.spring_courier.core.pipelines.IdempotencyBehavior;
import io.github.valossa515.spring_courier.core.pipelines.LoggingBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.pipelines.RetryBehavior;
import io.github.valossa515.spring_courier.core.support.BehaviorDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.DefaultResponseEntityConverter;
import io.github.valossa515.spring_courier.core.support.HandlerDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
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
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Role;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Autoconfiguration that wires the core Spring Courier beans required for the
 * CQRS pipeline and notification support. All beans are registered conditionally
 * so users can provide their own implementations.
 */
@Configuration
@EnableConfigurationProperties(CourierProperties.class)
@ImportRuntimeHints(CourierRuntimeHints.class)
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
    public static ProcessorRegistry processorRegistry() {
        return new ProcessorRegistry();
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
                           ProcessorRegistry processorRegistry,
                           CourierProperties properties,
                           ObjectProvider<Executor> asyncExecutor) {
        return new Courier(handlerRegistry, notificationRegistry,
                pipelineExecutor, processorRegistry,
                asyncExecutor.getIfUnique(),
                properties.getAsyncTimeoutMs(),
                properties.getNotificationStrategy());
    }

    @Bean
    @ConditionalOnMissingBean
    public ResponseEntityConverter responseEntityConverter() {
        return new DefaultResponseEntityConverter();
    }

    /**
     * Built-in logging behavior. Disabled with
     * {@code spring.courier.logging.enabled=false}.
     */
    @Bean
    @ConditionalOnMissingBean(LoggingBehavior.class)
    @ConditionalOnProperty(name = "spring.courier.logging.enabled",
            havingValue = "true", matchIfMissing = true)
    public LoggingBehavior<?, ?> loggingBehavior() {
        return new LoggingBehavior<>();
    }

    /**
     * Built-in caching behavior for queries. Disabled by default;
     * enable with {@code spring.courier.cache.enabled=true}.
     */
    @Bean
    @ConditionalOnMissingBean(CachingBehavior.class)
    @ConditionalOnProperty(name = "spring.courier.cache.enabled",
            havingValue = "true")
    public CachingBehavior<?, ?> cachingBehavior(
            CourierProperties properties) {
        CourierProperties.Cache cacheProps = properties.getCache();
        return new CachingBehavior<>(
                Duration.ofSeconds(cacheProps.getTtlSeconds()),
                cacheProps.getMaxSize());
    }

    /**
     * Built-in retry behavior with exponential backoff. Disabled by
     * default; enable with {@code spring.courier.retry.enabled=true}.
     */
    @Bean
    @ConditionalOnMissingBean(RetryBehavior.class)
    @ConditionalOnProperty(name = "spring.courier.retry.enabled",
            havingValue = "true")
    public RetryBehavior<?, ?> retryBehavior(
            CourierProperties properties) {
        CourierProperties.Retry retryProps = properties.getRetry();
        return new RetryBehavior<>(
                retryProps.getMaxAttempts(),
                retryProps.getDelayMs(),
                retryProps.getMultiplier());
    }

    /**
     * Built-in idempotency behavior. Disabled by default;
     * enable with {@code spring.courier.idempotency.enabled=true}.
     */
    @Bean
    @ConditionalOnMissingBean(IdempotencyBehavior.class)
    @ConditionalOnProperty(name = "spring.courier.idempotency.enabled",
            havingValue = "true")
    public IdempotencyBehavior<?, ?> idempotencyBehavior(
            CourierProperties properties) {
        return new IdempotencyBehavior<>(
                properties.getIdempotency().getMaxSize());
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
            ProcessorRegistry processorRegistry) {
        return new ProcessorDiscoveryPostProcessor(processorRegistry);
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
            ProcessorRegistry processorRegistry) {
        return () -> {
            handlerRegistry.freeze();
            notificationRegistry.freeze();
            pipelineRegistry.freeze();
            processorRegistry.freeze();
        };
    }
}
