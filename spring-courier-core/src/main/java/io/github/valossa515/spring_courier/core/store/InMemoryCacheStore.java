package io.github.valossa515.spring_courier.core.store;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
public class InMemoryCacheStore implements CacheStore {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryCacheStore.class);

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxSize;

    /**
     * Creates a store.
     *
     * @param maxSize maximum number of entries (0 or less = unlimited)
     */
    public InMemoryCacheStore(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public Optional<Object> get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (maxSize > 0 && entries.size() >= maxSize) {
            evictExpired();
            if (entries.size() >= maxSize) {
                LOG.debug("Cache full ({}/{}), not storing key {}", entries.size(), maxSize, key);
                return;
            }
        }
        entries.put(key, new Entry(value, expiresAt(ttl)));
    }

    @Override
    public void invalidateAll() {
        entries.clear();
    }

    @Override
    public void invalidateByPrefix(String keyPrefix) {
        entries.keySet().removeIf(k -> k.startsWith(keyPrefix));
    }

    @Override
    public long size() {
        return entries.size();
    }

    private static long expiresAt(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Long.MAX_VALUE;
        }
        long now = System.currentTimeMillis();
        long ttlMs = ttl.toMillis();
        // Saturate instead of overflowing into the past.
        return (Long.MAX_VALUE - now < ttlMs) ? Long.MAX_VALUE : now + ttlMs;
    }

    private void evictExpired() {
        entries.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private record Entry(Object value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
