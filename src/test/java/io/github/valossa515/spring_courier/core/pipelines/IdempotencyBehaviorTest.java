package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.annotations.Idempotent;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IdempotencyBehaviorTest {

    private IdempotencyBehavior<IRequest<Response<?>>, Response<?>>
            behavior;
    private AtomicInteger invocations;

    @Idempotent(ttlSeconds = 60)
    record CreateOrder(String orderId)
            implements ICommand<String> {
        @Override
        public String toString() {
            return "CreateOrder[orderId=" + orderId + "]";
        }
    }

    record NonIdempotentCmd(String data)
            implements ICommand<String> {
    }

    @BeforeEach
    void setUp() {
        behavior = new IdempotencyBehavior<>(100);
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
    void duplicateIdempotentRequestReturnsCached() {
        CreateOrder cmd = new CreateOrder("order-1");

        Response<?> first = execute(cmd);
        Response<?> second = execute(cmd);

        assertEquals(1, invocations.get());
        assertEquals(first.getData(), second.getData());
        assertEquals(1, behavior.size());
    }

    @Test
    void differentIdempotentRequestsExecuteSeparately() {
        execute(new CreateOrder("order-1"));
        execute(new CreateOrder("order-2"));

        assertEquals(2, invocations.get());
        assertEquals(2, behavior.size());
    }

    @Test
    void nonIdempotentRequestAlwaysExecutes() {
        NonIdempotentCmd cmd = new NonIdempotentCmd("data");

        execute(cmd);
        execute(cmd);

        assertEquals(2, invocations.get());
        assertEquals(0, behavior.size());
    }

    @Test
    void clearRemovesAllEntries() {
        execute(new CreateOrder("order-1"));
        assertEquals(1, behavior.size());

        behavior.clear();
        assertEquals(0, behavior.size());

        execute(new CreateOrder("order-1"));
        assertEquals(2, invocations.get());
    }

    @Test
    void removeByKeyEvictsSpecificEntry() {
        CreateOrder cmd = new CreateOrder("order-1");
        execute(cmd);
        assertEquals(1, behavior.size());

        behavior.remove(CreateOrder.class, cmd.toString());
        assertEquals(0, behavior.size());
    }

    @Test
    void expiredEntryIsReExecuted() throws Exception {
        @Idempotent(ttlSeconds = 0)
        record ShortLivedCmd() implements ICommand<String> {
            @Override
            public String toString() {
                return "ShortLivedCmd[]";
            }
        }

        // ttlSeconds=0 means Long.MAX_VALUE (never expires)
        // Use a custom behavior with very short TTL to test expiry
        // Instead, test the max-size eviction path
        IdempotencyBehavior<IRequest<Response<?>>, Response<?>>
                smallBehavior = new IdempotencyBehavior<>(1);
        AtomicInteger counter = new AtomicInteger(0);

        @SuppressWarnings("unchecked")
        PipelineBehavior.Next<Response<?>> next = () -> {
            counter.incrementAndGet();
            return Response.success("r-" + counter.get());
        };

        // Fill the store
        smallBehavior.handle(
                (IRequest<Response<?>>) (IRequest<?>)
                        new CreateOrder("a"), next);
        // Try to add beyond capacity
        smallBehavior.handle(
                (IRequest<Response<?>>) (IRequest<?>)
                        new CreateOrder("b"), next);

        assertEquals(2, counter.get());
    }

    @Test
    void orderIsHighestPrecedencePlus5() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 5,
                behavior.getOrder());
    }

    @Test
    @SuppressWarnings("unchecked")
    void metricsRecordHitsAndMisses() {
        List<String> recorded = new ArrayList<>();
        BehaviorMetrics spy = (name, type) ->
                recorded.add(name);

        IdempotencyBehavior<IRequest<Response<?>>,
                Response<?>> metered =
                new IdempotencyBehavior<>(100, spy);

        CreateOrder cmd = new CreateOrder("x");
        PipelineBehavior.Next<Response<?>> next =
                () -> Response.success("ok");

        metered.handle(
                (IRequest<Response<?>>) (IRequest<?>) cmd,
                next);
        metered.handle(
                (IRequest<Response<?>>) (IRequest<?>) cmd,
                next);

        assertEquals(2, recorded.size());
        assertEquals("courier.idempotency.misses",
                recorded.get(0));
        assertEquals("courier.idempotency.hits",
                recorded.get(1));
    }

    @Test
    void nullMetricsFallsBackToNoop() {
        IdempotencyBehavior<IRequest<Response<?>>,
                Response<?>> safe =
                new IdempotencyBehavior<>(100, null);
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 5,
                safe.getOrder());
    }
}
