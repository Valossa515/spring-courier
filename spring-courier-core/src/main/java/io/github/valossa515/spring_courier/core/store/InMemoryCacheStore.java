package io.github.valossa515.spring_courier.core.store;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Heap-backed {@link CacheStore} — the default cache backend.
 *
 * <p>Entries are bounded by {@code maxSize}: once the store is full, expired
 * entries are evicted and, if that does not free room, the new entry is
 * dropped rather than evicting a live one.
 *
 * <p>Because the entries live in the JVM heap, each application instance keeps
 * its own copy. Use a distributed {@link CacheStore} when results must be
 * shared across instances.
 */
public class InMemoryCacheStore extends AbstractMemoryStore implements CacheStore {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryCacheStore.class);

    /**
     * Creates a store.
     *
     * @param maxSize maximum number of entries (0 or less = unlimited)
     */
    public InMemoryCacheStore(int maxSize) {
        super(maxSize);
    }

    @Override
    public Optional<Object> get(String key) {
        return getValue(key);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        putValue(key, value, ttl, "Cache", LOG);
    }

    @Override
    public void invalidateAll() {
        clearValues();
    }

    @Override
    public void invalidateByPrefix(String keyPrefix) {
        removeByPrefix(keyPrefix);
    }

    @Override
    public long size() {
        return sizeValue();
    }
}
