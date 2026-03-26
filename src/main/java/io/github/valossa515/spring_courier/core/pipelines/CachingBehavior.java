package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.metrics.CourierMetrics;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline behavior that caches {@link IQuery} responses in memory.
 * Commands ({@link io.github.valossa515.spring_courier.core.interfaces.ICommand})
 * always bypass the cache.
 *
 * <p>Cache entries are keyed by the query's {@code toString()} value
 * (which works out of the box with Java {@code record} types).
 * Entries expire after a configurable TTL.
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

    private final Map<String, CacheEntry> cache =
            new ConcurrentHashMap<>();
    private final long ttlMs;
    private final int maxSize;
    private final BehaviorMetrics metrics;

    /**
     * Creates a caching behavior with given TTL and max size.
     *
     * @param ttl     time-to-live for cache entries
     * @param maxSize maximum number of entries (0 = unlimited)
     */
    public CachingBehavior(Duration ttl, int maxSize) {
        this(ttl, maxSize, BehaviorMetrics.NOOP);
    }

    /**
     * Creates a caching behavior with metrics support.
     *
     * @param ttl     time-to-live for cache entries
     * @param maxSize maximum number of entries (0 = unlimited)
     * @param metrics recorder for cache hit/miss counters
     */
    public CachingBehavior(Duration ttl, int maxSize,
            BehaviorMetrics metrics) {
        this.ttlMs = ttl.toMillis();
        this.maxSize = maxSize;
        this.metrics = metrics != null
                ? metrics : BehaviorMetrics.NOOP;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S handle(R request, Next<S> next) {
        if (!(request instanceof IQuery<?>)) {
            return next.invoke();
        }

        String key = request.getClass().getName()
                + ":" + request.toString();
        String requestType =
                request.getClass().getSimpleName();

        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            logger.debug("Cache HIT for {}", key);
            metrics.incrementCounter(
                    CourierMetrics.CACHE_HITS, requestType);
            return (S) entry.value();
        }

        S result = next.invoke();

        if (maxSize > 0 && cache.size() >= maxSize) {
            evictExpired();
            if (cache.size() >= maxSize) {
                logger.debug(
                        "Cache full ({}/{}), skipping cache for {}",
                        cache.size(), maxSize, key);
                metrics.incrementCounter(
                        CourierMetrics.CACHE_MISSES,
                        requestType);
                return result;
            }
        }

        cache.put(key, new CacheEntry(result,
                System.currentTimeMillis() + ttlMs));
        logger.debug("Cache MISS — stored {}", key);
        metrics.incrementCounter(
                CourierMetrics.CACHE_MISSES, requestType);
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
        cache.clear();
        logger.debug("Cache invalidated (all entries removed)");
    }

    /**
     * Invalidates entries matching the given request type.
     *
     * @param requestType the request class to evict
     */
    public void invalidate(Class<?> requestType) {
        String prefix = requestType.getName() + ":";
        cache.keySet().removeIf(k -> k.startsWith(prefix));
        logger.debug("Cache invalidated for {}", requestType.getSimpleName());
    }

    /**
     * Returns the current number of cached entries.
     */
    public int size() {
        return cache.size();
    }

    private void evictExpired() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private record CacheEntry(Object value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
