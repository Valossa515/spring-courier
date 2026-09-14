package io.github.valossa515.spring_courier.cache.redis;

import io.github.valossa515.spring_courier.core.store.CacheStore;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * {@link CacheStore} backed by Redis, so cached query results are shared by
 * every application instance instead of living in each instance's heap.
 *
 * <p>Values are serialized by the configured {@link RedisTemplate}. The default
 * template wired by {@link RedisStoreAutoConfiguration} uses
 * {@code GenericJackson2JsonRedisSerializer}, which embeds the concrete type in
 * the JSON so the value can be read back as its original class. Anything a
 * handler returns must therefore be serializable by that serializer.
 *
 * <p>Entry expiry is delegated to Redis (key TTL), so there is no background
 * eviction and no {@code maxSize} — bound the keyspace with Redis'
 * {@code maxmemory} policy instead.
 */
public class RedisCacheStore extends AbstractRedisStore implements CacheStore {

    /**
     * Creates a Redis-backed cache store.
     *
     * @param template  template used to talk to Redis
     * @param keyPrefix prefix applied to every key (namespacing)
     */
    public RedisCacheStore(RedisTemplate<String, Object> template, String keyPrefix) {
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
    public void invalidateAll() {
        clearValues();
    }

    @Override
    public void invalidateByPrefix(String keyPrefix) {
        invalidateByPrefixValue(keyPrefix);
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
