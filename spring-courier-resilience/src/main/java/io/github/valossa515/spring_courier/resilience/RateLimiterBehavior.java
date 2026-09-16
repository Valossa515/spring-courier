package io.github.valossa515.spring_courier.resilience;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * Pipeline behavior that caps how many requests of a given type may run per
 * time window.
 *
 * <p>Runs at {@code HIGHEST_PRECEDENCE + 130}: after validation
 * ({@code + 100}), so malformed requests are rejected before they consume
 * quota, and before the circuit breaker and retry, so throttling happens as
 * early as the pipeline usefully allows.
 *
 * <p>Limiter instances are looked up by the request's simple class name, so each
 * request type gets its own budget and can be tuned through the standard
 * Resilience4j configuration, e.g.:
 *
 * <pre>
 *   resilience4j.ratelimiter.instances.GetProductByIdQuery.limit-for-period=100
 * </pre>
 *
 * <p>When the budget is exhausted Resilience4j throws
 * {@link RequestNotPermitted}; {@link ResilienceExceptionHandler} maps it to a
 * {@code 429} response.
 *
 * @param <R> request type
 * @param <S> response type
 */
public class RateLimiterBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(RateLimiterBehavior.class);

    private final RateLimiterRegistry registry;
    private final BehaviorMetrics metrics;

    /**
     * Creates the behavior.
     *
     * @param registry registry the per-request-type limiters come from
     * @param metrics  recorder for rejection counters
     */
    public RateLimiterBehavior(RateLimiterRegistry registry,
            BehaviorMetrics metrics) {
        this.registry = registry;
        this.metrics = metrics != null ? metrics : BehaviorMetrics.NOOP;
    }

    @Override
    public S handle(R request, Next<S> next) {
        String requestType = request.getClass().getSimpleName();
        RateLimiter limiter = registry.rateLimiter(requestType);

        try {
            return limiter.executeSupplier(next::invoke);
        } catch (RequestNotPermitted rejected) {
            metrics.incrementCounter(ResilienceMetrics.RATE_LIMITED, requestType);
            logger.warn("Rate limit exhausted for {} — call rejected", requestType);
            throw rejected;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 130;
    }
}
