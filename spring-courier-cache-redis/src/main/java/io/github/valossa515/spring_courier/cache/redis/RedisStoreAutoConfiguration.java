package io.github.valossa515.spring_courier.cache.redis;

import io.github.valossa515.spring_courier.core.store.CacheStore;
import io.github.valossa515.spring_courier.core.store.IdempotencyStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Auto-configuration that swaps Spring Courier's in-memory cache and
 * idempotency stores for Redis-backed ones, so both are shared across
 * application instances.
 *
 * <p>Activates when {@code spring.courier.redis.enabled=true}, Spring Data
 * Redis is on the classpath and a {@link RedisConnectionFactory} bean exists.
 * The core auto-configuration picks up whichever {@link CacheStore} /
 * {@link IdempotencyStore} bean is present, so nothing else has to change.
 *
 * <p>Every bean is {@link ConditionalOnMissingBean}, so an application can
 * supply its own template (e.g. a different serializer) or store.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({RedisTemplate.class, RedisConnectionFactory.class})
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "spring.courier.redis", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RedisStoreProperties.class)
public class RedisStoreAutoConfiguration {

    /** Bean name of the template used by the Courier stores. */
    public static final String TEMPLATE_BEAN = "courierRedisTemplate";

    /**
     * Template dedicated to Spring Courier: String keys and JSON values with
     * embedded type information, so a value is read back as its original type.
     *
     * <p>Named distinctly so it never collides with Spring Boot's own
     * {@code redisTemplate}.
     */
    @Bean(TEMPLATE_BEAN)
    @ConditionalOnMissingBean(name = TEMPLATE_BEAN)
    public RedisTemplate<String, Object> courierRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(CacheStore.class)
    @ConditionalOnProperty(prefix = "spring.courier.redis", name = "cache-enabled",
            havingValue = "true", matchIfMissing = true)
    public CacheStore redisCacheStore(
            @Qualifier(TEMPLATE_BEAN) RedisTemplate<String, Object> template,
            RedisStoreProperties properties) {
        return new RedisCacheStore(template, properties.getKeyPrefix() + "cache:");
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyStore.class)
    @ConditionalOnProperty(prefix = "spring.courier.redis", name = "idempotency-enabled",
            havingValue = "true", matchIfMissing = true)
    public IdempotencyStore redisIdempotencyStore(
            @Qualifier(TEMPLATE_BEAN) RedisTemplate<String, Object> template,
            RedisStoreProperties properties) {
        return new RedisIdempotencyStore(template, properties.getKeyPrefix() + "idem:");
    }
}
