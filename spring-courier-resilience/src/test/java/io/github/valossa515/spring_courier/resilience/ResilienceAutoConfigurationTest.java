package io.github.valossa515.spring_courier.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ResilienceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResilienceAutoConfiguration.class));

    @Test
    void backsOffWhenDisabled() {
        runner.run(ctx -> assertThat(ctx)
                .doesNotHaveBean(CircuitBreakerBehavior.class)
                .doesNotHaveBean(RateLimiterBehavior.class)
                .doesNotHaveBean(BulkheadBehavior.class)
                .doesNotHaveBean(ResilienceExceptionHandler.class));
    }

    @Test
    void enablesOnlyTheCircuitBreakerByDefault() {
        runner.withPropertyValues("spring.courier.resilience.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(CircuitBreakerBehavior.class);
                    assertThat(ctx).hasSingleBean(ResilienceExceptionHandler.class);
                    assertThat(ctx)
                            .as("rate limiter and bulkhead impose hard ceilings, so they are opt-in")
                            .doesNotHaveBean(RateLimiterBehavior.class)
                            .doesNotHaveBean(BulkheadBehavior.class);
                });
    }

    @Test
    void rateLimiterAndBulkheadCanBeTurnedOn() {
        runner.withPropertyValues(
                        "spring.courier.resilience.enabled=true",
                        "spring.courier.resilience.rate-limiter-enabled=true",
                        "spring.courier.resilience.bulkhead-enabled=true")
                .run(ctx -> assertThat(ctx)
                        .hasSingleBean(RateLimiterBehavior.class)
                        .hasSingleBean(BulkheadBehavior.class));
    }

    @Test
    void circuitBreakerCanBeTurnedOff() {
        runner.withPropertyValues(
                        "spring.courier.resilience.enabled=true",
                        "spring.courier.resilience.circuit-breaker-enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(CircuitBreakerBehavior.class));
    }

    @Test
    void anApplicationSuppliedRegistryWins() {
        CircuitBreakerRegistry custom = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .waitDurationInOpenState(Duration.ofSeconds(42))
                .build());

        runner.withPropertyValues("spring.courier.resilience.enabled=true")
                .withBean(CircuitBreakerRegistry.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(CircuitBreakerRegistry.class)).isSameAs(custom));
    }
}
