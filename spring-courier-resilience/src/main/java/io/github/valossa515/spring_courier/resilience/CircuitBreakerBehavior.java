package io.github.valossa515.spring_courier.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * Pipeline behavior that trips a circuit breaker per request type, so a failing
 * dependency stops being called instead of being hammered.
 *
 * <p><strong>Ordering matters.</strong> This runs at
 * {@code HIGHEST_PRECEDENCE + 140}, which is <em>outside</em>
 * {@code RetryBehavior} ({@code + 150}). With the breaker open, a call is
 * rejected once rather than being retried N times — retrying into a dependency
 * that is already failing multiplies load on it and prolongs the outage. The
 * cost of this choice is that the breaker records one failure per request
 * rather than one per attempt; that is the intended trade, since the goal is to
 * shed load rather than to measure attempts.
 *
 * <p>Breaker instances are looked up by the request's simple class name, so each
 * request type gets its own circuit and can be tuned individually through the
 * standard Resilience4j configuration, e.g.:
 *
 * <pre>
 *   resilience4j.circuitbreaker.instances.CreateOrderCommand.failure-rate-threshold=50
 * </pre>
 *
 * <p>When the circuit is open, Resilience4j throws
 * {@link CallNotPermittedException}. {@link ResilienceExceptionHandler} turns
 * that into a {@code 503} response instead of a generic 500.
 *
 * @param <R> request type
 * @param <S> response type
 */
public class CircuitBreakerBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(CircuitBreakerBehavior.class);

    private final CircuitBreakerRegistry registry;
    private final BehaviorMetrics metrics;

    /**
     * Creates the behavior.
     *
     * @param registry registry the per-request-type breakers come from
     * @param metrics  recorder for rejection counters
     */
    public CircuitBreakerBehavior(CircuitBreakerRegistry registry,
            BehaviorMetrics metrics) {
        this.registry = registry;
        this.metrics = metrics != null ? metrics : BehaviorMetrics.NOOP;
    }

    @Override
    public S handle(R request, Next<S> next) {
        String requestType = request.getClass().getSimpleName();
        CircuitBreaker breaker = registry.circuitBreaker(requestType);

        try {
            return breaker.executeSupplier(next::invoke);
        } catch (CallNotPermittedException rejected) {
            metrics.incrementCounter(ResilienceMetrics.CIRCUIT_REJECTED, requestType);
            logger.warn("Circuit breaker is {} for {} — call rejected without invoking the handler",
                    breaker.getState(), requestType);
            throw rejected;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 140;
    }
}
