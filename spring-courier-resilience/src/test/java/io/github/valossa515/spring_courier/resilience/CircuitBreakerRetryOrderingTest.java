package io.github.valossa515.spring_courier.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.RetryBehavior;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Pins down the reason {@link CircuitBreakerBehavior} is ordered outside
 * {@code RetryBehavior}, by running both arrangements against an open circuit
 * and comparing how many calls each one lets through.
 */
class CircuitBreakerRetryOrderingTest {

    private static final int MAX_ATTEMPTS = 3;

    private final SampleRequest request = new SampleRequest("r-1");

    private CircuitBreakerRegistry openCircuit() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(1)
                .minimumNumberOfCalls(1)
                .failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
        registry.circuitBreaker(SampleRequest.class.getSimpleName()).transitionToOpenState();
        return registry;
    }

    private RetryBehavior<SampleRequest, String> retry() {
        return new RetryBehavior<>(MAX_ATTEMPTS, 1, 1.0);
    }

    private CircuitBreakerBehavior<SampleRequest, String> breaker(CircuitBreakerRegistry registry) {
        return new CircuitBreakerBehavior<>(registry, BehaviorMetrics.NOOP);
    }

    @Test
    void breakerOutsideRetryRejectsOnceAndNeverReachesTheHandler() {
        var breaker = breaker(openCircuit());
        var retry = retry();
        AtomicInteger handlerCalls = new AtomicInteger();
        AtomicInteger downstreamCalls = new AtomicInteger();

        PipelineBehavior.Next<String> handler = () -> {
            handlerCalls.incrementAndGet();
            return "never";
        };

        // This is the shipped ordering: breaker (140) wraps retry (150).
        assertThatThrownBy(() -> breaker.handle(request, () -> {
            downstreamCalls.incrementAndGet();
            return retry.handle(request, handler);
        })).isInstanceOf(CallNotPermittedException.class);

        assertThat(downstreamCalls.get())
                .as("an open circuit short-circuits before retry even starts")
                .isZero();
        assertThat(handlerCalls.get()).isZero();
    }

    @Test
    void retryOutsideBreakerWouldAmplifyCallsAgainstAnOpenCircuit() {
        var breaker = breaker(openCircuit());
        var retry = retry();
        AtomicInteger breakerCalls = new AtomicInteger();

        // The rejected alternative: retry (150) wrapping the breaker.
        assertThatThrownBy(() -> retry.handle(request, () -> {
            breakerCalls.incrementAndGet();
            return breaker.handle(request, () -> "never");
        })).isInstanceOf(CallNotPermittedException.class);

        assertThat(breakerCalls.get())
                .as("retry re-attempts a call the circuit already refused, "
                        + "multiplying load during an outage — the reason for the chosen order")
                .isEqualTo(MAX_ATTEMPTS);
    }
}
