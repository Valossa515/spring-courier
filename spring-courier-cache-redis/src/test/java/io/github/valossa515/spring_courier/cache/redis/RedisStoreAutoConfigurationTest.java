package io.github.valossa515.spring_courier.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.valossa515.spring_courier.core.store.CacheStore;
import io.github.valossa515.spring_courier.core.store.IdempotencyStore;
import io.github.valossa515.spring_courier.core.store.InMemoryCacheStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class RedisStoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisStoreAutoConfiguration.class))
            .withUserConfiguration(RedisInfra.class);

    @Test
    void registersRedisStoresWhenEnabled() {
        runner.withPropertyValues("spring.courier.redis.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(CacheStore.class);
                    assertThat(ctx).hasSingleBean(IdempotencyStore.class);
                    assertThat(ctx.getBean(CacheStore.class)).isInstanceOf(RedisCacheStore.class);
                    assertThat(ctx.getBean(IdempotencyStore.class))
                            .isInstanceOf(RedisIdempotencyStore.class);
                });
    }

    @Test
    void backsOffWhenDisabled() {
        runner.run(ctx -> assertThat(ctx)
                .doesNotHaveBean(CacheStore.class)
                .doesNotHaveBean(IdempotencyStore.class));
    }

    @Test
    void cacheCanStayInMemoryWhileIdempotencyUsesRedis() {
        runner.withPropertyValues(
                        "spring.courier.redis.enabled=true",
                        "spring.courier.redis.cache-enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(CacheStore.class);
                    assertThat(ctx).hasSingleBean(IdempotencyStore.class);
                });
    }

    @Test
    void idempotencyCanStayInMemoryWhileCacheUsesRedis() {
        runner.withPropertyValues(
                        "spring.courier.redis.enabled=true",
                        "spring.courier.redis.idempotency-enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(CacheStore.class);
                    assertThat(ctx).doesNotHaveBean(IdempotencyStore.class);
                });
    }

    @Test
    void applicationSuppliedStoreWins() {
        CacheStore custom = new InMemoryCacheStore(10);

        runner.withPropertyValues("spring.courier.redis.enabled=true")
                .withBean(CacheStore.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(CacheStore.class)).isSameAs(custom));
    }

    @Test
    void honorsCustomKeyPrefix() {
        runner.withPropertyValues(
                        "spring.courier.redis.enabled=true",
                        "spring.courier.redis.key-prefix=myapp:")
                .run(ctx -> assertThat(ctx.getBean(RedisStoreProperties.class).getKeyPrefix())
                        .isEqualTo("myapp:"));
    }

    @Configuration(proxyBeanMethods = false)
    static class RedisInfra {
        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }
    }
}
