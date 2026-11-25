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
 * <p>A store does two jobs. It records the results of finished executions
 * ({@link #get}/{@link #put}), which serves duplicates arriving
 * <em>after</em> the first one completed. And it claims keys for executions
 * that are still running ({@link #tryClaim}/{@link #releaseClaim}), which stops
 * a duplicate arriving <em>while</em> the first one runs on another instance —
 * the result is not recorded until the handler returns, so the result alone
 * cannot cover that window.
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

    /**
     * Atomically reserves {@code key} for an execution about to start.
     *
     * <p>Exactly one concurrent caller may win, across every instance that
     * shares the store; the claim lapses on its own after {@code ttl}, so an
     * instance that dies mid-execution cannot block the key forever. The claim
     * is independent of the recorded result: {@link #get} never sees it.
     *
     * <p>The default returns {@code true} unconditionally, which keeps stores
     * written before this method existed working exactly as they did — with
     * no protection against concurrent duplicates on different instances.
     * Distributed stores should override it with an atomic primitive (Redis
     * {@code SET NX PX}, a unique insert, and so on).
     *
     * @param key idempotency key
     * @param ttl how long the claim holds if never released; always positive
     * @return {@code true} if the caller now holds the claim, {@code false} if
     *         another execution already holds it
     */
    default boolean tryClaim(String key, Duration ttl) {
        return true;
    }

    /**
     * Releases a claim taken with {@link #tryClaim}.
     *
     * <p>Callers record the result with {@link #put} <em>before</em> releasing,
     * so the next claimant can find it. Releasing a claim that is not held, or
     * that already expired, must be a no-op.
     *
     * @param key idempotency key
     */
    default void releaseClaim(String key) {
        // Nothing to release: the default tryClaim holds nothing.
    }
}
