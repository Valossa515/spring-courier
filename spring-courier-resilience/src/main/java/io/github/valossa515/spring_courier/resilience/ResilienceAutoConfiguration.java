package io.github.valossa515.spring_courier.resilience;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the Resilience4j-backed pipeline behaviors.
 *
 * <p>Activates when {@code spring.courier.resilience.enabled=true} and
 * Resilience4j is on the classpath. Each behavior has its own switch; see
 * {@link ResilienceProperties} for why only the circuit breaker defaults to on.
 *
 * <p>The registries are {@link ConditionalOnMissingBean}, so an application
 * already using the Resilience4j Spring Boot starter keeps its own
 * property-configured registries and these behaviors simply read from them.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({CircuitBreakerRegistry.class, RateLimiterRegistry.class, BulkheadRegistry.class})
@ConditionalOnProperty(prefix = "spring.courier.resilience", name = "enabled",
        havingValue = "true")
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry courierCircuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterRegistry courierRateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public BulkheadRegistry courierBulkheadRegistry() {
        return BulkheadRegistry.ofDefaults();
    }

    @Bean
    @ConditionalOnMissingBean(CircuitBreakerBehavior.class)
    @ConditionalOnProperty(prefix = "spring.courier.resilience",
            name = "circuit-breaker-enabled", havingValue = "true", matchIfMissing = true)
    public CircuitBreakerBehavior<?, ?> circuitBreakerBehavior(
            CircuitBreakerRegistry registry,
            ObjectProvider<BehaviorMetrics> metricsProvider) {
        return new CircuitBreakerBehavior<>(registry,
                metricsProvider.getIfAvailable(() -> BehaviorMetrics.NOOP));
    }

    @Bean
    @ConditionalOnMissingBean(RateLimiterBehavior.class)
    @ConditionalOnProperty(prefix = "spring.courier.resilience",
            name = "rate-limiter-enabled", havingValue = "true")
    public RateLimiterBehavior<?, ?> rateLimiterBehavior(
            RateLimiterRegistry registry,
            ObjectProvider<BehaviorMetrics> metricsProvider) {
        return new RateLimiterBehavior<>(registry,
                metricsProvider.getIfAvailable(() -> BehaviorMetrics.NOOP));
    }

    @Bean
    @ConditionalOnMissingBean(BulkheadBehavior.class)
    @ConditionalOnProperty(prefix = "spring.courier.resilience",
            name = "bulkhead-enabled", havingValue = "true")
    public BulkheadBehavior<?, ?> bulkheadBehavior(
            BulkheadRegistry registry,
            ObjectProvider<BehaviorMetrics> metricsProvider) {
        return new BulkheadBehavior<>(registry,
                metricsProvider.getIfAvailable(() -> BehaviorMetrics.NOOP));
    }

    /**
     * Maps Resilience4j rejections to 503/429 instead of a masked 500.
     */
    @Bean
    @ConditionalOnMissingBean(ResilienceExceptionHandler.class)
    public ResilienceExceptionHandler resilienceExceptionHandler() {
        return new ResilienceExceptionHandler();
    }
}
