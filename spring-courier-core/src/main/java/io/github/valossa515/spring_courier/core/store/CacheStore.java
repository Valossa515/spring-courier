package io.github.valossa515.spring_courier.core.store;

import java.time.Duration;
import java.util.Optional;

/**
 * Storage backend for {@link io.github.valossa515.spring_courier.core.pipelines.CachingBehavior}.
 *
 * <p>The default implementation ({@link InMemoryCacheStore}) keeps entries in
 * the JVM heap, which means each application instance has its own cache. Swap
 * in a distributed implementation (e.g. the {@code spring-courier-cache-redis}
 * module) to share cached results across instances.
 *
 * <p>Implementations must be thread-safe.
 */
public interface CacheStore {

    /**
     * Looks up a cached value.
     *
     * @param key cache key
     * @return the cached value, or {@link Optional#empty()} on a miss
     *         (including expired entries)
     */
    Optional<Object> get(String key);

    /**
     * Stores a value under {@code key}, expiring after {@code ttl}.
     *
     * <p>Implementations may silently decline to store (e.g. a bounded store
     * that is full); callers must not depend on a subsequent {@link #get} hit.
     *
     * @param key   cache key
     * @param value value to cache; never {@code null}
     * @param ttl   time-to-live; a non-positive value means "no expiry"
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Removes every entry from the store.
     */
    void invalidateAll();

    /**
     * Removes every entry whose key starts with {@code keyPrefix}.
     *
     * @param keyPrefix prefix to match
     */
    void invalidateByPrefix(String keyPrefix);

    /**
     * Returns the number of entries currently held, or {@code -1} when the
     * backend cannot report it cheaply.
     */
    long size();
}
