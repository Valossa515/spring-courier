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
 * instance. A duplicate that arrives <em>while</em> the first is still running
 * on another instance finds the first one's claim ({@link #tryClaim}, an atomic
 * {@code SET NX PX}) and is rejected instead of executing.
 *
 * <p>Claims live under {@code <keyPrefix>#claim:<key>}. Idempotency keys
 * start with the request's class name, which cannot contain {@code #}, so a
 * claim key never collides with a result key, and {@link #clear()} — which
 * deletes everything under the prefix — removes both.
 */
public class RedisIdempotencyStore extends AbstractRedisStore implements IdempotencyStore {

    private static final String CLAIM_NAMESPACE = "#claim:";

    /** Value stored under a claim key; only its presence matters. */
    private static final String CLAIM_MARKER = "claimed";

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

    /**
     * Claims the key with {@code SET key value NX PX ttl}: Redis applies the
     * existence check and the write as one command, so exactly one instance
     * wins, and the expiry is set in that same command — there is no window in
     * which a claim exists without a TTL and could outlive a dead holder.
     *
     * <p>{@code setIfAbsent} only returns {@code null} inside a pipeline or a
     * transaction, which this store never opens; it is treated as "not
     * claimed", the safe side.
     */
    @Override
    public boolean tryClaim(String key, Duration ttl) {
        Boolean claimed = template.opsForValue()
                .setIfAbsent(claimKey(key), CLAIM_MARKER, ttl);
        return Boolean.TRUE.equals(claimed);
    }

    @Override
    public void releaseClaim(String key) {
        template.delete(claimKey(key));
    }

    private String claimKey(String key) {
        return keyPrefix + CLAIM_NAMESPACE + key;
    }
}
