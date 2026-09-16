package io.github.valossa515.spring_courier.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * Pipeline behavior that caps how many requests of a given type may execute
 * concurrently, so one slow dependency cannot consume the whole thread pool.
 *
 * <p><strong>Ordering matters.</strong> This runs at
 * {@code HIGHEST_PRECEDENCE + 160}, which is <em>inside</em>
 * {@code RetryBehavior} ({@code + 150}): a permit is acquired per attempt and
 * released before the retry backoff sleeps, instead of one permit being held
 * across the whole retry sequence. Holding a permit while a retry sleeps would
 * starve the pool with calls that are not doing any work.
 *
 * <p>Bulkhead instances are looked up by the request's simple class name, so each
 * request type gets its own pool and can be tuned through the standard
 * Resilience4j configuration, e.g.:
 *
 * <pre>
 *   resilience4j.bulkhead.instances.GenerateReportQuery.max-concurrent-calls=5
 * </pre>
 *
 * <p>When the bulkhead is saturated Resilience4j throws
 * {@link BulkheadFullException}; {@link ResilienceExceptionHandler} maps it to a
 * {@code 503} response.
 *
 * @param <R> request type
 * @param <S> response type
 */
public class BulkheadBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(BulkheadBehavior.class);

    private final BulkheadRegistry registry;
    private final BehaviorMetrics metrics;

    /**
     * Creates the behavior.
     *
     * @param registry registry the per-request-type bulkheads come from
     * @param metrics  recorder for rejection counters
     */
    public BulkheadBehavior(BulkheadRegistry registry,
            BehaviorMetrics metrics) {
        this.registry = registry;
        this.metrics = metrics != null ? metrics : BehaviorMetrics.NOOP;
    }

    @Override
    public S handle(R request, Next<S> next) {
        String requestType = request.getClass().getSimpleName();
        Bulkhead bulkhead = registry.bulkhead(requestType);

        try {
            return bulkhead.executeSupplier(next::invoke);
        } catch (BulkheadFullException rejected) {
            metrics.incrementCounter(ResilienceMetrics.BULKHEAD_REJECTED, requestType);
            logger.warn("Bulkhead saturated for {} — call rejected", requestType);
            throw rejected;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 160;
    }
}
