package io.github.valossa515.spring_courier.core.pipelines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.valossa515.spring_courier.annotations.Idempotent;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.store.IdempotencyStore;
import io.github.valossa515.spring_courier.core.store.InMemoryIdempotencyStore;
import io.github.valossa515.spring_courier.core.support.Response;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Two {@link IdempotencyBehavior} instances sharing one store model two
 * application instances sharing one Redis: each behavior keeps its own
 * per-JVM in-flight map, so only the store can stop a duplicate that lands on
 * the other instance while the first is still running.
 */
class IdempotencyCrossInstanceTest {

    @Idempotent(ttlSeconds = 60)
    record Charge(String paymentId) implements ICommand<String> { }

    private IdempotencyStore sharedStore;
    private IdempotencyBehavior<IRequest<Response<?>>, Response<?>> podA;
    private IdempotencyBehavior<IRequest<Response<?>>, Response<?>> podB;
    private AtomicInteger executions;
    private ExecutorService pool;

    @BeforeEach
    void setUp() {
        sharedStore = new InMemoryIdempotencyStore(100);
        podA = new IdempotencyBehavior<>(sharedStore, BehaviorMetrics.NOOP);
        podB = new IdempotencyBehavior<>(sharedStore, BehaviorMetrics.NOOP);
        executions = new AtomicInteger();
        pool = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @SuppressWarnings("unchecked")
    private static IRequest<Response<?>> charge(String id) {
        return (IRequest<Response<?>>) (IRequest<?>) new Charge(id);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("latch timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    /** Starts the request on pod A and parks it mid-execution. */
    private Future<Response<?>> startOnPodAAndHold(CountDownLatch release) {
        CountDownLatch started = new CountDownLatch(1);
        Future<Response<?>> first = pool.submit(() -> podA.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            started.countDown();
            await(release);
            return Response.success("charged");
        }));
        await(started);
        return first;
    }

    @Test
    void aDuplicateOnAnotherInstanceDoesNotExecuteWhileTheFirstIsRunning() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        Future<Response<?>> first = startOnPodAAndHold(release);

        Response<?> duplicate = podB.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged again");
        });

        release.countDown();
        first.get(5, TimeUnit.SECONDS);

        assertThat(executions.get())
                .as("a double click or a client retry that lands on another pod must not "
                        + "run the handler a second time")
                .isEqualTo(1);
        assertThat(duplicate.isSuccess()).isFalse();
        assertThat(duplicate.getStatusCode())
                .as("409 tells the client the request is already being processed — retrying "
                        + "later returns the recorded result")
                .isEqualTo(409);
    }

    @Test
    void aRetryOnAnotherInstanceAfterTheFirstFinishedGetsTheRecordedResult() {
        podA.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged");
        });

        Response<?> retry = podB.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged again");
        });

        assertThat(executions.get()).isEqualTo(1);
        assertThat(retry.getData()).isEqualTo("charged");
    }

    /**
     * The window the post-claim re-check exists for: this instance reads the
     * store before the other one records its result, and claims after the
     * other one released. Without the re-check it would run the handler again.
     */
    @Test
    void aResultThatLandsBetweenTheMissAndTheClaimIsNotExecutedAgain() {
        InMemoryIdempotencyStore real = new InMemoryIdempotencyStore(100);
        real.put(Charge.class.getName() + ":" + new Charge("p-1"),
                Response.success("charged"), Duration.ofMinutes(1));
        IdempotencyStore staleFirstRead = new DelegatingStore(real) {
            private boolean firstRead = true;

            @Override
            public Optional<Object> get(String key) {
                if (firstRead) {
                    firstRead = false;
                    return Optional.empty();
                }
                return super.get(key);
            }
        };
        var pod = new IdempotencyBehavior<IRequest<Response<?>>, Response<?>>(
                staleFirstRead, BehaviorMetrics.NOOP);

        Response<?> response = pod.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged again");
        });

        assertThat(executions.get()).isZero();
        assertThat(response.getData()).isEqualTo("charged");
    }

    @Test
    void aFailedExecutionReleasesTheClaimSoTheRequestCanBeRetried() {
        try {
            podA.handle(charge("p-1"), () -> {
                executions.incrementAndGet();
                throw new IllegalStateException("gateway down");
            });
        } catch (IllegalStateException expected) {
            // the failure propagates, as before
        }

        Response<?> retry = podB.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged");
        });

        assertThat(retry.isSuccess())
                .as("a claim left behind by a failure would turn every retry into a 409 "
                        + "until it expired")
                .isTrue();
        assertThat(executions.get()).isEqualTo(2);
    }

    @Test
    void anErrorResponseReleasesTheClaimAndIsNotRecorded() {
        podA.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.error("card declined", 402);
        });

        Response<?> retry = podB.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged");
        });

        assertThat(retry.isSuccess()).isTrue();
        assertThat(executions.get()).isEqualTo(2);
    }

    /**
     * An instance that dies mid-execution never releases its claim. The TTL is
     * what keeps the key from being blocked forever.
     */
    @Test
    void aClaimLeftByADeadInstanceExpires() throws Exception {
        var pod = new IdempotencyBehavior<IRequest<Response<?>>, Response<?>>(
                sharedStore, BehaviorMetrics.NOOP, Duration.ofMillis(100));
        String key = Charge.class.getName() + ":" + new Charge("p-1");
        assertThat(sharedStore.tryClaim(key, Duration.ofMillis(100))).isTrue();

        Response<?> whileHeld = pod.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged");
        });
        Thread.sleep(150);
        Response<?> afterExpiry = pod.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged");
        });

        assertThat(whileHeld.getStatusCode()).isEqualTo(409);
        assertThat(afterExpiry.isSuccess()).isTrue();
        assertThat(executions.get()).isEqualTo(1);
    }

    /**
     * Pins the compatibility promise: a store written before claims existed
     * inherits {@code tryClaim() == true} and behaves exactly as it used to.
     */
    @Test
    void aStoreWithoutClaimSupportKeepsThePreviousBehavior() throws Exception {
        IdempotencyStore legacy = new LegacyStore(new InMemoryIdempotencyStore(100));
        podA = new IdempotencyBehavior<>(legacy, BehaviorMetrics.NOOP);
        podB = new IdempotencyBehavior<>(legacy, BehaviorMetrics.NOOP);

        CountDownLatch release = new CountDownLatch(1);
        Future<Response<?>> first = startOnPodAAndHold(release);
        Response<?> duplicate = podB.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged again");
        });
        release.countDown();
        first.get(5, TimeUnit.SECONDS);

        assertThat(duplicate.isSuccess()).isTrue();
        assertThat(executions.get()).isEqualTo(2);
    }

    @Test
    void aFailureToReleaseTheClaimDoesNotReplaceTheResult() {
        IdempotencyStore flakyRelease = new DelegatingStore(new InMemoryIdempotencyStore(100)) {
            @Override
            public void releaseClaim(String key) {
                throw new IllegalStateException("redis blip");
            }
        };
        var pod = new IdempotencyBehavior<IRequest<Response<?>>, Response<?>>(
                flakyRelease, BehaviorMetrics.NOOP);

        Response<?> response = pod.handle(charge("p-1"), () -> {
            executions.incrementAndGet();
            return Response.success("charged");
        });

        assertThat(response.getData())
                .as("the handler already ran and its side effects happened; reporting an "
                        + "error now would invite a retry that the recorded result then hides")
                .isEqualTo("charged");
    }

    @Test
    void rejectsANonPositiveClaimTtl() {
        assertThatThrownBy(
                () -> new IdempotencyBehavior<>(sharedStore, BehaviorMetrics.NOOP,
                        Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * A store as a third party would have written it before claims existed:
     * only the original five methods, so it inherits the interface defaults.
     */
    private static final class LegacyStore implements IdempotencyStore {
        private final IdempotencyStore results;

        LegacyStore(IdempotencyStore results) {
            this.results = results;
        }

        @Override
        public Optional<Object> get(String key) {
            return results.get(key);
        }

        @Override
        public void put(String key, Object value, Duration ttl) {
            results.put(key, value, ttl);
        }

        @Override
        public void remove(String key) {
            results.remove(key);
        }

        @Override
        public void clear() {
            results.clear();
        }

        @Override
        public long size() {
            return results.size();
        }
    }

    /** Forwards everything, so a test can override just the part it needs. */
    private static class DelegatingStore implements IdempotencyStore {
        private final IdempotencyStore delegate;

        DelegatingStore(IdempotencyStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<Object> get(String key) {
            return delegate.get(key);
        }

        @Override
        public void put(String key, Object value, Duration ttl) {
            delegate.put(key, value, ttl);
        }

        @Override
        public void remove(String key) {
            delegate.remove(key);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public long size() {
            return delegate.size();
        }

        @Override
        public boolean tryClaim(String key, Duration ttl) {
            return delegate.tryClaim(key, ttl);
        }

        @Override
        public void releaseClaim(String key) {
            delegate.releaseClaim(key);
        }
    }
}
