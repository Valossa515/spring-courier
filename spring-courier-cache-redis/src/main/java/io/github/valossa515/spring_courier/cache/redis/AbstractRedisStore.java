package io.github.valossa515.spring_courier.cache.redis;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;

abstract class AbstractRedisStore {

    protected final RedisTemplate<String, Object> template;
    protected final String keyPrefix;

    protected AbstractRedisStore(RedisTemplate<String, Object> template, String keyPrefix) {
        this.template = template;
        this.keyPrefix = keyPrefix;
    }

    protected Optional<Object> getValue(String key) {
        return Optional.ofNullable(template.opsForValue().get(fullKey(key)));
    }

    protected void putValue(String key, Object value, Duration ttl) {
        String redisKey = fullKey(key);
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            template.opsForValue().set(redisKey, value);
        } else {
            template.opsForValue().set(redisKey, value, ttl);
        }
    }

    protected void removeValue(String key) {
        template.delete(fullKey(key));
    }

    protected void clearValues() {
        RedisKeys.deleteByPattern(template, keyPrefix);
    }

    protected void invalidateByPrefixValue(String prefix) {
        RedisKeys.deleteByPattern(template, keyPrefix + prefix);
    }

    protected String fullKey(String key) {
        return keyPrefix + key;
    }
}
