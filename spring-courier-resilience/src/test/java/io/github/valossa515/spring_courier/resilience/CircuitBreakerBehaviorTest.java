package io.github.valossa515.spring_courier.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

class CircuitBreakerBehaviorTest {

    /** RetryBehavior's order in core; the breaker must sit outside it. */
    private static final int RETRY_ORDER = Ordered.HIGHEST_PRECEDENCE + 150;

    private final SampleRequest request = new SampleRequest("r-1");

    private CircuitBreakerRegistry tripsAfterTwoFailures() {
        return CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
    }

    private CircuitBreakerBehavior<SampleRequest, String> behavior(CircuitBreakerRegistry registry) {
        return new CircuitBreakerBehavior<>(registry, BehaviorMetrics.NOOP);
    }

    @Test
    void passesThroughWhileClosed() {
        var behavior = behavior(CircuitBreakerRegistry.ofDefaults());

        assertThat(behavior.handle(request, () -> "ok")).isEqualTo("ok");
    }

    @Test
    void rejectsWithoutInvokingTheHandlerOnceOpen() {
        var behavior = behavior(tripsAfterTwoFailures());
        AtomicInteger handlerCalls = new AtomicInteger();

        // Two failures trip the breaker.
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> behavior.handle(request, () -> {
                handlerCalls.incrementAndGet();
                throw new IllegalStateException("dependency down");
            })).isInstanceOf(IllegalStateException.class);
        }
        assertThat(handlerCalls.get()).isEqualTo(2);

        // Now the circuit is open: the handler must not be reached at all.
        assertThatThrownBy(() -> behavior.handle(request, () -> {
            handlerCalls.incrementAndGet();
            return "should not run";
        })).isInstanceOf(CallNotPermittedException.class);

        assertThat(handlerCalls.get())
                .as("handler must not be invoked while the circuit is open")
                .isEqualTo(2);
    }

    @Test
    void keepsACircuitPerRequestType() {
        CircuitBreakerRegistry registry = tripsAfterTwoFailures();
        var behavior = new CircuitBreakerBehavior<SampleRequest, String>(registry, BehaviorMetrics.NOOP);

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> behavior.handle(request, () -> {
                throw new IllegalStateException("down");
            })).isInstanceOf(IllegalStateException.class);
        }

        // A different request type has its own circuit, still closed.
        var otherBehavior = new CircuitBreakerBehavior<OtherRequest, String>(registry, BehaviorMetrics.NOOP);
        assertThat(otherBehavior.handle(new OtherRequest(), () -> "ok")).isEqualTo("ok");
    }

    @Test
    void runsOutsideRetrySoAnOpenCircuitIsNotRetried() {
        assertThat(behavior(CircuitBreakerRegistry.ofDefaults()).getOrder())
                .as("breaker must be outermost relative to RetryBehavior, "
                        + "otherwise an open circuit would still be retried N times")
                .isLessThan(RETRY_ORDER);
    }

    /** Second request type, to prove circuits are not shared. */
    record OtherRequest() implements io.github.valossa515.spring_courier.core.interfaces.IRequest<String> {
    }
}
