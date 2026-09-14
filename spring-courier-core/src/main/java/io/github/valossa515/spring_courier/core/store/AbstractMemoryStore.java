package io.github.valossa515.spring_courier.core.store;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

abstract class AbstractMemoryStore {

    protected final Map<String, Entry> entries = new ConcurrentHashMap<>();
    protected final int maxSize;

    protected AbstractMemoryStore(int maxSize) {
        this.maxSize = maxSize;
    }

    protected Optional<Object> getValue(String key) {
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

    protected void putValue(String key, Object value, Duration ttl, String kind, Logger log) {
        if (maxSize > 0 && entries.size() >= maxSize) {
            evictExpired();
            if (entries.size() >= maxSize) {
                log.debug("{} store full ({}/{}), not storing key {}", kind, entries.size(), maxSize, key);
                return;
            }
        }
        entries.put(key, new Entry(value, expiresAt(ttl)));
    }

    protected void removeValue(String key) {
        entries.remove(key);
    }

    protected void clearValues() {
        entries.clear();
    }

    protected void removeByPrefix(String keyPrefix) {
        entries.keySet().removeIf(k -> k.startsWith(keyPrefix));
    }

    protected long sizeValue() {
        return entries.size();
    }

    protected void evictExpired() {
        entries.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    protected static long expiresAt(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Long.MAX_VALUE;
        }
        long now = System.currentTimeMillis();
        long ttlMs = ttl.toMillis();
        return (Long.MAX_VALUE - now < ttlMs) ? Long.MAX_VALUE : now + ttlMs;
    }

    protected record Entry(Object value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
