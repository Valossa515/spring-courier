package io.github.valossa515.spring_courier.core.store;

import java.time.Duration;
import java.util.Optional;

/**
 * Storage backend for
 * {@link io.github.valossa515.spring_courier.core.pipelines.IdempotencyBehavior}.
 *
 * <p>The default implementation ({@link InMemoryIdempotencyStore}) keeps
 * entries in the JVM heap, so a duplicate request that lands on a different
 * instance is <em>not</em> deduplicated. Swap in a distributed implementation
 * (e.g. the {@code spring-courier-cache-redis} module) to deduplicate across
 * instances.
 *
 * <p>Implementations must be thread-safe.
 */
public interface IdempotencyStore {

    /**
     * Looks up a previously recorded result.
     *
     * @param key idempotency key
     * @return the recorded result, or {@link Optional#empty()} when the key is
     *         unknown or expired
     */
    Optional<Object> get(String key);

    /**
     * Records the result of a successful execution.
     *
     * <p>Implementations may silently decline to store (e.g. a bounded store
     * that is full); callers must not depend on a subsequent {@link #get} hit.
     *
     * @param key   idempotency key
     * @param value result to record; never {@code null}
     * @param ttl   time-to-live; a non-positive value means "no expiry"
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Removes a single entry.
     *
     * @param key idempotency key
     */
    void remove(String key);

    /**
     * Removes every entry from the store.
     */
    void clear();

    /**
     * Returns the number of entries currently held, or {@code -1} when the
     * backend cannot report it cheaply.
     */
    long size();
}
