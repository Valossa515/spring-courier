package io.github.valossa515.spring_courier.core.store;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Heap-backed {@link IdempotencyStore} — the default idempotency backend.
 *
 * <p>Entries are bounded by {@code maxSize}: once the store is full, expired
 * entries are evicted and, if that does not free room, the new entry is
 * dropped rather than evicting a live one.
 *
 * <p>Because the entries live in the JVM heap, a duplicate request handled by
 * another instance will execute again. Use a distributed
 * {@link IdempotencyStore} when that matters.
 */
public class InMemoryIdempotencyStore extends AbstractMemoryStore implements IdempotencyStore {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryIdempotencyStore.class);

    /**
     * Claim expiry (epoch millis) per key. Kept apart from the recorded
     * results so a claim never counts against {@code maxSize} and is never
     * returned by {@link #get}.
     */
    private final Map<String, Long> claims = new ConcurrentHashMap<>();

    /**
     * Creates a store.
     *
     * @param maxSize maximum number of entries (0 or less = unlimited)
     */
    public InMemoryIdempotencyStore(int maxSize) {
        super(maxSize);
    }

    @Override
    public Optional<Object> get(String key) {
        return getValue(key);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        putValue(key, value, ttl, "Idempotency", LOG);
    }

    @Override
    public void remove(String key) {
        removeValue(key);
    }

    @Override
    public void clear() {
        clearValues();
        claims.clear();
    }

    /**
     * Claims the key unless a live claim exists. Implemented for real rather
     * than left to the interface default: within one JVM the behavior's own
     * in-flight map already covers duplicates, but a faithful implementation
     * lets the cross-instance logic be exercised by sharing one store between
     * two behaviors, without a Redis.
     */
    @Override
    public boolean tryClaim(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        long expiresAt = now + ttl.toMillis();
        boolean[] won = {false};
        // compute() runs atomically per key, so the check and the write cannot
        // interleave with another caller's.
        claims.compute(key, (k, current) -> {
            if (current == null || current <= now) {
                won[0] = true;
                return expiresAt;
            }
            return current;
        });
        return won[0];
    }

    @Override
    public void releaseClaim(String key) {
        claims.remove(key);
    }

    @Override
    public long size() {
        return sizeValue();
    }
}
