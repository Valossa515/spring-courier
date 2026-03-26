package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.annotations.Idempotent;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.metrics.CourierMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline behavior that enforces idempotency for requests
 * annotated with {@link Idempotent}.
 *
 * <p>When a request class carries {@code @Idempotent}, this
 * behavior uses the request's {@code toString()} as the
 * idempotency key. If a cached response exists and has not
 * expired, it is returned immediately without invoking the
 * handler.
 *
 * <p>Requests without the annotation pass through untouched.
 *
 * <p>Enabled via {@code spring.courier.idempotency.enabled=true}
 * (disabled by default).
 *
 * @param <R> request type
 * @param <S> response type
 */
public class IdempotencyBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(IdempotencyBehavior.class);

    private final Map<String, IdempotencyEntry> store =
            new ConcurrentHashMap<>();
    private final int maxSize;
    private final BehaviorMetrics metrics;

    /**
     * Creates an idempotency behavior.
     *
     * @param maxSize maximum number of stored entries (0 = unlimited)
     */
    public IdempotencyBehavior(int maxSize) {
        this(maxSize, BehaviorMetrics.NOOP);
    }

    /**
     * Creates an idempotency behavior with metrics support.
     *
     * @param maxSize maximum number of stored entries (0 = unlimited)
     * @param metrics recorder for idempotency hit/miss counters
     */
    public IdempotencyBehavior(int maxSize,
            BehaviorMetrics metrics) {
        this.maxSize = maxSize;
        this.metrics = metrics != null
                ? metrics : BehaviorMetrics.NOOP;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S handle(R request, Next<S> next) {
        Idempotent annotation = request.getClass()
                .getAnnotation(Idempotent.class);
        if (annotation == null) {
            return next.invoke();
        }

        String key = request.getClass().getName()
                + ":" + request.toString();
        String requestType =
                request.getClass().getSimpleName();

        IdempotencyEntry entry = store.get(key);
        if (entry != null && !entry.isExpired()) {
            logger.debug("Idempotent HIT for {}", key);
            metrics.incrementCounter(
                    CourierMetrics.IDEMPOTENCY_HITS,
                    requestType);
            return (S) entry.response();
        }

        S result = next.invoke();

        long expiresAt;
        if (annotation.ttlSeconds() > 0) {
            long ttlMs = annotation.ttlSeconds() * 1000L;
            long now = System.currentTimeMillis();
            expiresAt = (Long.MAX_VALUE - now < ttlMs)
                    ? Long.MAX_VALUE : now + ttlMs;
        } else {
            expiresAt = Long.MAX_VALUE;
        }

        if (maxSize > 0 && store.size() >= maxSize) {
            evictExpired();
            if (store.size() >= maxSize) {
                logger.debug(
                        "Idempotency store full ({}/{}), "
                                + "skipping storage for {}",
                        store.size(), maxSize, key);
                metrics.incrementCounter(
                        CourierMetrics.IDEMPOTENCY_MISSES,
                        requestType);
                return result;
            }
        }

        store.put(key, new IdempotencyEntry(result, expiresAt));
        logger.debug("Idempotent MISS — stored {}", key);
        metrics.incrementCounter(
                CourierMetrics.IDEMPOTENCY_MISSES,
                requestType);
        return result;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    /**
     * Clears all stored idempotency entries.
     */
    public void clear() {
        store.clear();
    }

    /**
     * Returns the current number of stored entries.
     */
    public int size() {
        return store.size();
    }

    /**
     * Removes a specific entry by request key.
     *
     * @param requestType the request class
     * @param requestKey  the request's toString() value
     */
    public void remove(Class<?> requestType, String requestKey) {
        store.remove(requestType.getName() + ":" + requestKey);
    }

    private void evictExpired() {
        store.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private record IdempotencyEntry(Object response,
            long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
