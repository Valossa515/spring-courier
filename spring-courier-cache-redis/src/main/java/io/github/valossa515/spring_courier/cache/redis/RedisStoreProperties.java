package io.github.valossa515.spring_courier.cache.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the Redis-backed stores, under the
 * {@code spring.courier.redis} prefix.
 *
 * <pre>
 *   spring.courier.redis.enabled=true
 *   spring.courier.redis.key-prefix=courier:
 *   spring.courier.redis.cache-enabled=true
 *   spring.courier.redis.idempotency-enabled=true
 * </pre>
 *
 * <p>These only choose the <em>backend</em>. The behaviors themselves are still
 * switched on by {@code spring.courier.cache.enabled} and
 * {@code spring.courier.idempotency.enabled}.
 */
@ConfigurationProperties(prefix = "spring.courier.redis")
public class RedisStoreProperties {

    /** Master switch for the Redis-backed stores. Disabled by default. */
    private boolean enabled = false;

    /** Prefix applied to every key written by Spring Courier. */
    private String keyPrefix = "courier:";

    /** Back the caching behavior with Redis instead of the in-memory store. */
    private boolean cacheEnabled = true;

    /** Back the idempotency behavior with Redis instead of the in-memory store. */
    private boolean idempotencyEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public boolean isIdempotencyEnabled() {
        return idempotencyEnabled;
    }

    public void setIdempotencyEnabled(boolean idempotencyEnabled) {
        this.idempotencyEnabled = idempotencyEnabled;
    }
}
