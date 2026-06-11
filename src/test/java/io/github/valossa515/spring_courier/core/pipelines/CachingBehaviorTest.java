package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachingBehaviorTest {

    private CachingBehavior<IRequest<Response<?>>, Response<?>>
            behavior;
    private AtomicInteger invocations;

    record GetUser(String id) implements IQuery<String> {
        @Override
        public String toString() {
            return "GetUser[id=" + id + "]";
        }
    }

    record CreateUser(String name) implements ICommand<String> {
        @Override
        public String toString() {
            return "CreateUser[name=" + name + "]";
        }
    }

    @BeforeEach
    void setUp() {
        behavior = new CachingBehavior<>(
                Duration.ofSeconds(60), 100);
        invocations = new AtomicInteger(0);
    }

    @SuppressWarnings("unchecked")
    private Response<?> execute(IRequest<?> request) {
        return behavior.handle(
                (IRequest<Response<?>>) request,
                () -> {
                    invocations.incrementAndGet();
                    return Response.success("result-"
                            + invocations.get());
                });
    }

    @Test
    void queryCachesResult() {
        GetUser query = new GetUser("1");

        Response<?> first = execute(query);
        Response<?> second = execute(query);

        assertEquals(1, invocations.get());
        assertEquals(first.getData(), second.getData());
    }

    @Test
    void differentQueriesHaveDifferentCacheEntries() {
        execute(new GetUser("1"));
        execute(new GetUser("2"));

        assertEquals(2, invocations.get());
        assertEquals(2, behavior.size());
    }

    @Test
    void commandsBypassCache() {
        CreateUser cmd = new CreateUser("John");

        execute(cmd);
        execute(cmd);

        assertEquals(2, invocations.get());
        assertEquals(0, behavior.size());
    }

    @Test
    void invalidateAllClearsCache() {
        execute(new GetUser("1"));
        assertEquals(1, behavior.size());

        behavior.invalidateAll();

        assertEquals(0, behavior.size());
        execute(new GetUser("1"));
        assertEquals(2, invocations.get());
    }

    @Test
    void invalidateByTypeRemovesMatchingEntries() {
        execute(new GetUser("1"));
        execute(new GetUser("2"));
        assertEquals(2, behavior.size());

        behavior.invalidate(GetUser.class);
        assertEquals(0, behavior.size());
    }

    @Test
    void expiredEntriesAreEvicted() throws Exception {
        CachingBehavior<IRequest<Response<?>>, Response<?>> shortTtl =
                new CachingBehavior<>(Duration.ofMillis(50), 100);
        AtomicInteger counter = new AtomicInteger(0);

        GetUser query = new GetUser("ttl-test");

        @SuppressWarnings("unchecked")
        PipelineBehavior.Next<Response<?>> next = () -> {
            counter.incrementAndGet();
            return Response.success("r-" + counter.get());
        };

        shortTtl.handle(
                (IRequest<Response<?>>) (IRequest<?>) query, next);
        Thread.sleep(100);
        shortTtl.handle(
                (IRequest<Response<?>>) (IRequest<?>) query, next);

        assertEquals(2, counter.get());
    }

    @Test
    void maxSizePreventsUnboundedGrowth() {
        CachingBehavior<IRequest<Response<?>>, Response<?>> small =
                new CachingBehavior<>(Duration.ofSeconds(60), 2);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            GetUser query = new GetUser("user-" + i);
            @SuppressWarnings("unchecked")
            IRequest<Response<?>> req =
                    (IRequest<Response<?>>) (IRequest<?>) query;
            small.handle(req, () -> {
                counter.incrementAndGet();
                return Response.success("ok");
            });
        }

        // Can't exceed maxSize
        assertEquals(true, small.size() <= 2);
    }

    @Test
    void orderIsHighestPrecedencePlus50() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 50,
                behavior.getOrder());
    }

    @Test
    @SuppressWarnings("unchecked")
    void metricsRecordHitsAndMisses() {
        List<String> recorded = new ArrayList<>();
        BehaviorMetrics spy = (name, type) ->
                recorded.add(name);

        CachingBehavior<IRequest<Response<?>>, Response<?>>
                metered = new CachingBehavior<>(
                Duration.ofSeconds(60), 100, spy);

        GetUser query = new GetUser("m1");
        PipelineBehavior.Next<Response<?>> next =
                () -> Response.success("ok");

        metered.handle(
                (IRequest<Response<?>>) (IRequest<?>) query,
                next);
        metered.handle(
                (IRequest<Response<?>>) (IRequest<?>) query,
                next);

        assertEquals(2, recorded.size());
        assertEquals("courier.cache.misses", recorded.get(0));
        assertEquals("courier.cache.hits", recorded.get(1));
    }

    @Test
    void nullMetricsFallsBackToNoop() {
        CachingBehavior<IRequest<Response<?>>, Response<?>>
                safe = new CachingBehavior<>(
                Duration.ofSeconds(60), 100, null);
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 50,
                safe.getOrder());
    }

    static class PlainQuery implements IQuery<String> {
    }

    @Test
    @SuppressWarnings("unchecked")
    void errorResponsesAreNotCached() {
        GetUser query = new GetUser("err");

        Response<?> first = behavior.handle(
                (IRequest<Response<?>>) (IRequest<?>) query,
                () -> {
                    invocations.incrementAndGet();
                    return Response.error("transient failure", 500);
                });
        Response<?> second = execute(query);

        assertFalse(first.isSuccess());
        assertTrue(second.isSuccess());
        assertEquals(2, invocations.get(),
                "Error response must not be served from cache");
        assertEquals(1, behavior.size());
    }

    @Test
    void queryWithoutCustomToStringIsNotCached() {
        PlainQuery query = new PlainQuery();

        execute(query);
        execute(query);

        assertEquals(2, invocations.get());
        assertEquals(0, behavior.size(),
                "Default Object.toString() keys would never match and "
                        + "would only fill the cache");
    }
}
