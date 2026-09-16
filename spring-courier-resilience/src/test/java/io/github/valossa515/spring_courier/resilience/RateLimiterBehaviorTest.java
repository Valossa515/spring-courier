package io.github.valossa515.spring_courier.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

class RateLimiterBehaviorTest {

    private final SampleRequest request = new SampleRequest("r-1");

    private RateLimiterBehavior<SampleRequest, String> oneCallPerMinute() {
        RateLimiterRegistry registry = RateLimiterRegistry.of(RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build());
        return new RateLimiterBehavior<>(registry, BehaviorMetrics.NOOP);
    }

    @Test
    void allowsCallsWithinTheBudget() {
        assertThat(oneCallPerMinute().handle(request, () -> "ok")).isEqualTo("ok");
    }

    @Test
    void rejectsOnceTheBudgetIsExhausted() {
        var behavior = oneCallPerMinute();

        assertThat(behavior.handle(request, () -> "first")).isEqualTo("first");

        assertThatThrownBy(() -> behavior.handle(request, () -> "second"))
                .isInstanceOf(RequestNotPermitted.class);
    }

    @Test
    void runsBeforeTheCircuitBreakerSoThrottlingHappensEarly() {
        assertThat(oneCallPerMinute().getOrder())
                .isLessThan(Ordered.HIGHEST_PRECEDENCE + 140);
    }

    @Test
    void runsAfterValidationSoInvalidRequestsDoNotBurnQuota() {
        assertThat(oneCallPerMinute().getOrder())
                .isGreaterThan(Ordered.HIGHEST_PRECEDENCE + 100);
    }
}
