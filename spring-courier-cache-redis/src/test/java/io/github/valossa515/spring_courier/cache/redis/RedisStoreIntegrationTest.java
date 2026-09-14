package io.github.valossa515.spring_courier.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import io.github.valossa515.spring_courier.core.support.Response;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Exercises the stores against a real Redis, which is the only way to verify
 * the serialization round trip the mock-based tests cannot cover.
 *
 * <p>Skips itself when no Redis is reachable on {@code localhost:6379}, so the
 * build stays green on machines and CI runners without one.
 */
class RedisStoreIntegrationTest {

    private static final String PREFIX = "courier-it:" + UUID.randomUUID() + ":";

    private static LettuceConnectionFactory factory;
    private static RedisTemplate<String, Object> template;

    @BeforeAll
    static void connect() {
        try {
            factory = new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration("localhost", 6379));
            factory.afterPropertiesSet();
            factory.getConnection().ping();
        } catch (Exception e) {
            abort("No Redis reachable on localhost:6379 — skipping integration test");
        }

        template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (template != null) {
            RedisKeys.deleteByPattern(template, PREFIX);
        }
        if (factory != null) {
            factory.destroy();
        }
    }

    private RedisCacheStore cacheStore() {
        return new RedisCacheStore(template, PREFIX + "cache:");
    }

    private RedisIdempotencyStore idempotencyStore() {
        return new RedisIdempotencyStore(template, PREFIX + "idem:");
    }

    @Test
    void roundTripsDomainRecordAsItsOriginalType() {
        RedisCacheStore store = cacheStore();
        Product original = new Product("p-1", 7);

        store.put("product", original, Duration.ofMinutes(1));

        assertThat(store.get("product")).contains(original);
    }

    @Test
    void roundTripsResponseReturnedByAHandler() {
        RedisCacheStore store = cacheStore();

        store.put("resp", Response.success(new Product("p-2", 3)), Duration.ofMinutes(1));

        Object restored = store.get("resp").orElseThrow();
        assertThat(restored).isInstanceOf(Response.class);
        Response<?> response = (Response<?>) restored;
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(new Product("p-2", 3));
    }

    @Test
    void entryDisappearsAfterTtl() throws InterruptedException {
        RedisCacheStore store = cacheStore();

        store.put("short", new Product("p-3", 1), Duration.ofMillis(300));
        assertThat(store.get("short")).isPresent();

        Thread.sleep(700);

        assertThat(store.get("short")).isEmpty();
    }

    @Test
    void invalidateByPrefixRemovesOnlyMatchingKeys() {
        RedisCacheStore store = cacheStore();
        store.put("com.example.A:1", new Product("a", 1), Duration.ofMinutes(1));
        store.put("com.example.B:1", new Product("b", 1), Duration.ofMinutes(1));

        store.invalidateByPrefix("com.example.A:");

        assertThat(store.get("com.example.A:1")).isEmpty();
        assertThat(store.get("com.example.B:1")).isPresent();
    }

    @Test
    void idempotencyStoreRecordsRemovesAndClears() {
        RedisIdempotencyStore store = idempotencyStore();
        store.put("k1", new Product("x", 1), Duration.ofMinutes(1));
        store.put("k2", new Product("y", 2), Duration.ofMinutes(1));

        assertThat(store.get("k1")).isPresent();

        store.remove("k1");
        assertThat(store.get("k1")).isEmpty();
        assertThat(store.get("k2")).isPresent();

        store.clear();
        assertThat(store.get("k2")).isEmpty();
    }

    /** Stand-in for whatever a handler returns. */
    public record Product(String id, int quantity) {
    }
}
