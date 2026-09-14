package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.annotations.Idempotent;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.metrics.CourierMetrics;
import io.github.valossa515.spring_courier.core.store.IdempotencyStore;
import io.github.valossa515.spring_courier.core.store.InMemoryIdempotencyStore;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline behavior that enforces idempotency for requests
 * annotated with {@link Idempotent}.
 *
 * <p>When a request class carries {@code @Idempotent}, this
 * behavior uses the request's {@code toString()} as the
 * idempotency key. If a recorded response exists and has not
 * expired, it is returned immediately without invoking the
 * handler. Request classes that do not override
 * {@code toString()} are skipped (with a one-time warning),
 * since their keys would never match.
 *
 * <p>Concurrent duplicates are deduplicated as well: while a
 * request for a given key is in flight, identical requests wait
 * for its outcome instead of executing the handler a second time.
 * This in-flight deduplication is <strong>per instance</strong>,
 * regardless of the configured {@link IdempotencyStore}.
 *
 * <p>Only successful results are stored — error
 * {@link Response Responses}, {@code null} results and exceptions
 * are never recorded, so a transient failure does not poison the
 * key for the TTL.
 *
 * <p>Recorded results are held by a pluggable {@link IdempotencyStore}.
 * The default is {@link InMemoryIdempotencyStore} (per-instance heap);
 * swap in a distributed store to deduplicate across instances.
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

    private final Map<String, CompletableFuture<Object>> inFlight =
            new ConcurrentHashMap<>();
    private final Set<Class<?>> unsupportedKeyWarned =
            ConcurrentHashMap.newKeySet();
    private final IdempotencyStore store;
    private final BehaviorMetrics metrics;

    /**
     * Creates an idempotency behavior backed by an in-memory store.
     *
     * @param maxSize maximum number of stored entries (0 = unlimited)
     */
    public IdempotencyBehavior(int maxSize) {
        this(maxSize, BehaviorMetrics.NOOP);
    }

    /**
     * Creates an idempotency behavior backed by an in-memory store,
     * with metrics.
     *
     * @param maxSize maximum number of stored entries (0 = unlimited)
     * @param metrics recorder for idempotency hit/miss counters
     */
    public IdempotencyBehavior(int maxSize,
            BehaviorMetrics metrics) {
        this(new InMemoryIdempotencyStore(maxSize), metrics);
    }

    /**
     * Creates an idempotency behavior backed by the given store.
     *
     * @param store   backend holding the recorded results
     * @param metrics recorder for idempotency hit/miss counters
     */
    public IdempotencyBehavior(IdempotencyStore store,
            BehaviorMetrics metrics) {
        this.store = store;
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

        Class<?> requestClass = request.getClass();
        if (!RequestKeySupport.hasCustomToString(requestClass)) {
            if (unsupportedKeyWarned.add(requestClass)) {
                logger.warn("@Idempotent request {} does not override "
                                + "toString(); idempotency is disabled for it. "
                                + "Use a record or override toString().",
                        requestClass.getSimpleName());
            }
            return next.invoke();
        }

        String key = requestClass.getName() + ":" + request;
        String requestType = requestClass.getSimpleName();

        Optional<Object> recorded = store.get(key);
        if (recorded.isPresent()) {
            logger.debug("Idempotent HIT for {}", requestType);
            metrics.incrementCounter(
                    CourierMetrics.IDEMPOTENCY_HITS,
                    requestType);
            return (S) recorded.get();
        }

        CompletableFuture<Object> execution = new CompletableFuture<>();
        CompletableFuture<Object> existing =
                inFlight.putIfAbsent(key, execution);
        if (existing != null) {
            logger.debug("Idempotent in-flight duplicate for {}, "
                    + "awaiting first execution", requestType);
            metrics.incrementCounter(
                    CourierMetrics.IDEMPOTENCY_HITS, requestType);
            return (S) awaitInFlight(existing);
        }

        try {
            S result = next.invoke();
            storeIfSuccessful(key, requestType, annotation, result);
            execution.complete(result);
            return result;
        } catch (RuntimeException | Error ex) {
            // Next.invoke() declares no checked exceptions, so this covers
            // every possible failure — waiters must never block forever
            execution.completeExceptionally(ex);
            throw ex;
        } finally {
            inFlight.remove(key, execution);
        }
    }

    /**
     * Waits for the first in-flight execution of the same key and
     * reuses its outcome, rethrowing its failure when it failed.
     */
    private Object awaitInFlight(CompletableFuture<Object> existing) {
        try {
            return existing.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    private void storeIfSuccessful(String key, String requestType,
            Idempotent annotation, Object result) {
        metrics.incrementCounter(
                CourierMetrics.IDEMPOTENCY_MISSES, requestType);

        if (result == null) {
            return;
        }

        if (result instanceof Response<?> response
                && !response.isSuccess()) {
            logger.debug("Idempotency skip for {} (error response)",
                    requestType);
            return;
        }

        Duration ttl = annotation.ttlSeconds() > 0
                ? Duration.ofSeconds(annotation.ttlSeconds())
                : Duration.ZERO;

        store.put(key, result, ttl);
        logger.debug("Idempotent MISS — stored {}", requestType);
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
     * Returns the current number of stored entries, or {@code -1} when the
     * backing store cannot report it.
     */
    public int size() {
        return (int) store.size();
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
}
