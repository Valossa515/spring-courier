package io.github.valossa515.spring_courier.core.store;

import java.time.Duration;
import java.util.Optional;
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
    }

    @Override
    public long size() {
        return sizeValue();
    }
}
