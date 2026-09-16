package io.github.valossa515.spring_courier.resilience;

/**
 * Metric names recorded by the resilience behaviors.
 *
 * <p>Kept in this module rather than in core's {@code CourierMetrics} so the
 * core artifact stays unaware of Resilience4j;
 * {@link io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics}
 * accepts any metric name.
 */
public final class ResilienceMetrics {

    /** Calls rejected because the circuit breaker was open. */
    public static final String CIRCUIT_REJECTED = "courier.circuitbreaker.rejected";

    /** Calls rejected because the rate limit was exhausted. */
    public static final String RATE_LIMITED = "courier.ratelimiter.rejected";

    /** Calls rejected because the bulkhead was saturated. */
    public static final String BULKHEAD_REJECTED = "courier.bulkhead.rejected";

    private ResilienceMetrics() {
    }
}
