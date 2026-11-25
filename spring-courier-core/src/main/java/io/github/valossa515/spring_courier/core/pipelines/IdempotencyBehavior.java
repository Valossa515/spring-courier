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
 * <p>Concurrent duplicates are deduplicated as well. On the same
 * instance, identical requests wait for the in-flight execution
 * and reuse its outcome. On <em>another</em> instance, the first
 * execution holds a claim in the shared {@link IdempotencyStore}
 * ({@link IdempotencyStore#tryClaim}), and a duplicate that finds
 * the claim taken is answered with {@code 409 Conflict} instead of
 * running the handler; retrying once the first execution finishes
 * returns its recorded result. The cross-instance part needs a
 * store that implements claims — the Redis module's does.
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

    /**
     * How long a claim holds when it is never released, i.e. when the
     * instance running the request dies. It must outlast the slowest
     * handler: a claim that lapses mid-execution lets a duplicate on
     * another instance run.
     */
    public static final Duration DEFAULT_CLAIM_TTL = Duration.ofSeconds(60);

    /** Status returned to a duplicate while another instance runs the request. */
    static final int CONFLICT_STATUS = 409;

    private final Map<String, CompletableFuture<Object>> inFlight =
            new ConcurrentHashMap<>();
    private final Set<Class<?>> unsupportedKeyWarned =
            ConcurrentHashMap.newKeySet();
    private final IdempotencyStore store;
    private final BehaviorMetrics metrics;
    private final Duration claimTtl;

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
        this(store, metrics, DEFAULT_CLAIM_TTL);
    }

    /**
     * Creates an idempotency behavior backed by the given store, with an
     * explicit claim TTL.
     *
     * @param store    backend holding the recorded results and claims
     * @param metrics  recorder for idempotency counters
     * @param claimTtl how long a claim holds if the instance holding it dies;
     *                 must be positive
     * @throws IllegalArgumentException if {@code claimTtl} is not positive
     */
    public IdempotencyBehavior(IdempotencyStore store,
            BehaviorMetrics metrics, Duration claimTtl) {
        if (claimTtl == null || claimTtl.isZero() || claimTtl.isNegative()) {
            throw new IllegalArgumentException(
                    "claimTtl must be positive, was " + claimTtl);
        }
        this.store = store;
        this.metrics = metrics != null
                ? metrics : BehaviorMetrics.NOOP;
        this.claimTtl = claimTtl;
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
            S result = executeClaimed(key, requestType, annotation, next);
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
     * Runs the handler under a claim in the shared store, so a duplicate
     * on another instance cannot run it at the same time.
     */
    @SuppressWarnings("unchecked")
    private S executeClaimed(String key, String requestType,
            Idempotent annotation, Next<S> next) {
        if (!store.tryClaim(key, claimTtl)) {
            logger.debug("Idempotent conflict for {}: already running "
                    + "on another instance", requestType);
            metrics.incrementCounter(
                    CourierMetrics.IDEMPOTENCY_CONFLICTS, requestType);
            // S is Response<?> in the dispatcher pipeline — the same
            // contract ValidationBehavior relies on for its 400.
            return (S) Response.error(
                    "Request is already being processed",
                    CONFLICT_STATUS, "IdempotencyConflict");
        }
        try {
            // The previous holder records its result before releasing
            // the claim. If it finished between our get() miss and this
            // claim, the result is visible now and must not run again.
            Optional<Object> landed = store.get(key);
            if (landed.isPresent()) {
                logger.debug("Idempotent HIT for {} after claim",
                        requestType);
                metrics.incrementCounter(
                        CourierMetrics.IDEMPOTENCY_HITS, requestType);
                return (S) landed.get();
            }

            S result = next.invoke();
            storeIfSuccessful(key, requestType, annotation, result);
            return result;
        } finally {
            releaseQuietly(key, requestType);
        }
    }

    /**
     * Releases the claim without letting a failure replace the outcome.
     * By now the handler has run and its side effects happened; turning
     * that into an error because the release failed would be worse than
     * leaving the claim to expire on its TTL.
     */
    private void releaseQuietly(String key, String requestType) {
        try {
            store.releaseClaim(key);
        } catch (RuntimeException e) {
            logger.warn("Could not release idempotency claim for {}; it "
                    + "will expire in {}: {}", requestType, claimTtl,
                    e.getMessage());
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
