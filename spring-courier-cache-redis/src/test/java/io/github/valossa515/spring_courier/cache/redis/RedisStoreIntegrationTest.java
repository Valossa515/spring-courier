package io.github.valossa515.spring_courier.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import io.github.valossa515.spring_courier.annotations.Idempotent;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import io.github.valossa515.spring_courier.core.pipelines.IdempotencyBehavior;
import io.github.valossa515.spring_courier.core.support.Response;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
 * build stays green on developer machines without one. CI sets
 * {@value #REQUIRE_REDIS_ENV}{@code =true} and provides Redis, and there a
 * missing server is a failure: these tests are the only check that the
 * idempotency claim really is atomic in Redis, and a silent skip would let a
 * broken service turn them off without anyone noticing.
 */
class RedisStoreIntegrationTest {

    static final String REQUIRE_REDIS_ENV = "COURIER_REQUIRE_REDIS";

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
            if (Boolean.parseBoolean(System.getenv(REQUIRE_REDIS_ENV))) {
                throw new IllegalStateException(REQUIRE_REDIS_ENV
                        + " is set but no Redis is reachable on localhost:6379", e);
            }
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

    // ── Claims ──────────────────────────────────────────────────────────

    @Test
    void claimsAreExclusiveAndReleasable() {
        RedisIdempotencyStore store = idempotencyStore();

        assertThat(store.tryClaim("claim-1", Duration.ofMinutes(1))).isTrue();
        assertThat(store.tryClaim("claim-1", Duration.ofMinutes(1))).isFalse();

        store.releaseClaim("claim-1");
        assertThat(store.tryClaim("claim-1", Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void aClaimExpiresOnItsOwn() throws InterruptedException {
        RedisIdempotencyStore store = idempotencyStore();
        store.tryClaim("claim-2", Duration.ofMillis(200));

        Thread.sleep(400);

        assertThat(store.tryClaim("claim-2", Duration.ofMinutes(1)))
                .as("the TTL is set by the same SET NX PX command, so a dead holder "
                        + "can never leave a claim without one")
                .isTrue();
    }

    @Test
    void aClaimIsInvisibleToGetAndClearedWithTheStore() {
        RedisIdempotencyStore store = idempotencyStore();
        store.tryClaim("claim-3", Duration.ofMinutes(1));

        assertThat(store.get("claim-3")).isEmpty();

        store.clear();
        assertThat(store.tryClaim("claim-3", Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void concurrentClaimersHaveExactlyOneWinner() throws Exception {
        RedisIdempotencyStore store = idempotencyStore();
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            AtomicInteger winners = new AtomicInteger();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    if (store.tryClaim("claim-4", Duration.ofMinutes(1))) {
                        winners.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }

            assertThat(winners.get()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Idempotent(ttlSeconds = 60)
    public record Charge(String paymentId) implements ICommand<String> {
    }

    /**
     * The scenario the claims exist for, end to end: two application
     * instances share one Redis, and a duplicate reaches the second instance
     * while the first is still running the handler.
     */
    @Test
    @SuppressWarnings("unchecked")
    void twoInstancesSharingRedisRunAConcurrentDuplicateOnlyOnce() throws Exception {
        RedisIdempotencyStore shared = idempotencyStore();
        var podA = new IdempotencyBehavior<IRequest<Response<?>>, Response<?>>(
                shared, BehaviorMetrics.NOOP);
        var podB = new IdempotencyBehavior<IRequest<Response<?>>, Response<?>>(
                shared, BehaviorMetrics.NOOP);
        IRequest<Response<?>> request =
                (IRequest<Response<?>>) (IRequest<?>) new Charge("pay-" + UUID.randomUUID());
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Response<?>> first = pool.submit(() -> podA.handle(request, () -> {
                executions.incrementAndGet();
                started.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Response.success(new Product("receipt", 1));
            }));
            assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();

            Response<?> duplicate = podB.handle(request, () -> {
                executions.incrementAndGet();
                return Response.success(new Product("second-receipt", 1));
            });
            release.countDown();
            first.get(10, TimeUnit.SECONDS);

            Response<?> retry = podB.handle(request, () -> {
                executions.incrementAndGet();
                return Response.success(new Product("third-receipt", 1));
            });

            assertThat(executions.get()).isEqualTo(1);
            assertThat(duplicate.getStatusCode()).isEqualTo(409);
            assertThat(retry.getData())
                    .as("the retry gets the first execution's result, round-tripped through Redis")
                    .isEqualTo(new Product("receipt", 1));
        } finally {
            pool.shutdownNow();
        }
    }

    /** Stand-in for whatever a handler returns. */
    public record Product(String id, int quantity) {
    }
}
