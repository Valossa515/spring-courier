package io.github.valossa515.spring_courier.cache.redis;

import io.github.valossa515.spring_courier.core.store.IdempotencyStore;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * {@link IdempotencyStore} backed by Redis, so a duplicate request is
 * recognised even when it lands on a different application instance.
 *
 * <p>Values are serialized by the configured {@link RedisTemplate} — see
 * {@link RedisCacheStore} for the serialization contract.
 *
 * <p><strong>Scope of the guarantee.</strong> A duplicate that arrives
 * <em>after</em> the first request finished is served from Redis on any
 * instance. Two duplicates running <em>concurrently on different instances</em>
 * can still both execute: the in-flight deduplication in
 * {@code IdempotencyBehavior} is per instance, and this store only records
 * results once a handler completes.
 */
public class RedisIdempotencyStore extends AbstractRedisStore implements IdempotencyStore {

    /**
     * Creates a Redis-backed idempotency store.
     *
     * @param template  template used to talk to Redis
     * @param keyPrefix prefix applied to every key (namespacing)
     */
    public RedisIdempotencyStore(RedisTemplate<String, Object> template, String keyPrefix) {
        super(template, keyPrefix);
    }

    @Override
    public Optional<Object> get(String key) {
        return getValue(key);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        putValue(key, value, ttl);
    }

    @Override
    public void remove(String key) {
        removeValue(key);
    }

    @Override
    public void clear() {
        clearValues();
    }

    /**
     * Always returns {@code -1}: counting keys in Redis means scanning the
     * keyspace, which is too expensive to do on every call.
     */
    @Override
    public long size() {
        return -1;
    }
}
