package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.metrics.CourierMetrics;
import io.github.valossa515.spring_courier.core.store.CacheStore;
import io.github.valossa515.spring_courier.core.store.InMemoryCacheStore;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline behavior that caches {@link IQuery} responses.
 * Commands ({@link io.github.valossa515.spring_courier.core.interfaces.ICommand})
 * always bypass the cache.
 *
 * <p>Cache entries are keyed by the query's {@code toString()} value
 * (which works out of the box with Java {@code record} types).
 * Query classes that do not override {@code toString()} are skipped
 * (with a one-time warning), since their keys would never match.
 * Entries expire after a configurable TTL.
 *
 * <p>Only successful results are cached — error
 * {@link Response Responses} always pass through uncached so transient
 * failures are not replayed for the duration of the TTL. {@code null}
 * results are never cached either.
 *
 * <p>Entries are held by a pluggable {@link CacheStore}. The default is
 * {@link InMemoryCacheStore} (per-instance heap); swap in a distributed store
 * to share cached results across application instances.
 *
 * <p>Enabled via {@code spring.courier.cache.enabled=true}
 * (disabled by default).
 *
 * @param <R> request type
 * @param <S> response type
 */
public class CachingBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(CachingBehavior.class);

    private final Set<Class<?>> unsupportedKeyWarned =
            ConcurrentHashMap.newKeySet();
    private final CacheStore store;
    private final Duration ttl;
    private final BehaviorMetrics metrics;

    /**
     * Creates a caching behavior backed by an in-memory store.
     *
     * @param ttl     time-to-live for cache entries
     * @param maxSize maximum number of entries (0 = unlimited)
     */
    public CachingBehavior(Duration ttl, int maxSize) {
        this(ttl, maxSize, BehaviorMetrics.NOOP);
    }

    /**
     * Creates a caching behavior backed by an in-memory store, with metrics.
     *
     * @param ttl     time-to-live for cache entries
     * @param maxSize maximum number of entries (0 = unlimited)
     * @param metrics recorder for cache hit/miss counters
     */
    public CachingBehavior(Duration ttl, int maxSize,
            BehaviorMetrics metrics) {
        this(new InMemoryCacheStore(maxSize), ttl, metrics);
    }

    /**
     * Creates a caching behavior backed by the given store.
     *
     * @param store   backend holding the cached entries
     * @param ttl     time-to-live for cache entries
     * @param metrics recorder for cache hit/miss counters
     */
    public CachingBehavior(CacheStore store, Duration ttl,
            BehaviorMetrics metrics) {
        this.store = store;
        this.ttl = ttl;
        this.metrics = metrics != null
                ? metrics : BehaviorMetrics.NOOP;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S handle(R request, Next<S> next) {
        if (!(request instanceof IQuery<?>)) {
            return next.invoke();
        }

        Class<?> requestClass = request.getClass();
        if (!RequestKeySupport.hasCustomToString(requestClass)) {
            if (unsupportedKeyWarned.add(requestClass)) {
                logger.warn("Query {} does not override toString(); "
                                + "caching is disabled for it. Use a record or "
                                + "override toString() to enable caching.",
                        requestClass.getSimpleName());
            }
            return next.invoke();
        }

        String key = requestClass.getName() + ":" + request;
        String requestType = requestClass.getSimpleName();

        Optional<Object> cached = store.get(key);
        if (cached.isPresent()) {
            logger.debug("Cache HIT for {}", requestType);
            metrics.incrementCounter(
                    CourierMetrics.CACHE_HITS, requestType);
            return (S) cached.get();
        }

        S result = next.invoke();
        metrics.incrementCounter(
                CourierMetrics.CACHE_MISSES, requestType);

        if (result == null) {
            return null;
        }

        if (result instanceof Response<?> response
                && !response.isSuccess()) {
            logger.debug("Cache skip for {} (error response)",
                    requestType);
            return result;
        }

        store.put(key, result, ttl);
        logger.debug("Cache MISS — stored {}", requestType);
        return result;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }

    /**
     * Invalidates all cached entries.
     */
    public void invalidateAll() {
        store.invalidateAll();
        logger.debug("Cache invalidated (all entries removed)");
    }

    /**
     * Invalidates entries matching the given request type.
     *
     * @param requestType the request class to evict
     */
    public void invalidate(Class<?> requestType) {
        store.invalidateByPrefix(requestType.getName() + ":");
        logger.debug("Cache invalidated for {}", requestType.getSimpleName());
    }

    /**
     * Returns the current number of cached entries, or {@code -1} when the
     * backing store cannot report it.
     */
    public int size() {
        return (int) store.size();
    }
}
