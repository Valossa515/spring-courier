package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CourierAdditionalTest {

    private CourierTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = CourierTestFixture.create();
    }

    @Test
    void sendHandlesCompletableFutureResult() {
        fixture.handlerRegistry().registerHandler(CfRequest.class, new CfHandler());

        Response<String> resp = fixture.courier().send(new CfRequest());
        assertTrue(resp.isSuccess());
        assertEquals("future-ok", resp.getData());
    }

    @Test
    void sendReturnsSuccessWithNullPayload() {
        fixture.handlerRegistry().registerHandler(NullRequest.class, new NullHandler());

        Response<String> resp = fixture.courier().send(new NullRequest());
        assertTrue(resp.isSuccess());
        assertFalse(resp.hasData());
        assertNull(resp.getData());
    }

    @Test
    void publishInvokesNonPublicHandlerWithAccessible() {
        AtomicInteger counter = new AtomicInteger();
        fixture.notificationRegistry().registerHandler(TestNotification.class, new PrivateNotificationHandler(counter));

        fixture.courier().publish(new TestNotification());
        assertEquals(1, counter.get());
    }

    static class CfRequest implements IRequest<String> {}
    static class CfHandler {
        public CompletableFuture<String> handle(CfRequest r) {
            Objects.requireNonNull(r);
            return CompletableFuture.completedFuture("future-ok");
        }
    }

    static class NullRequest implements IRequest<String> {}
    static class NullHandler {
        public String handle(NullRequest r) {
            Objects.requireNonNull(r);
            return null;
        }
    }

    static class TestNotification implements INotification {}
    static class PrivateNotificationHandler {
        private final AtomicInteger counter;
        PrivateNotificationHandler(AtomicInteger counter) { this.counter = counter; }
        public void handle(TestNotification n) {
            Objects.requireNonNull(n);
            counter.incrementAndGet();
        }
    }

    // --- Coverage for new constructors and configurable timeout ---

    @Test
    void courierAcceptsCustomAsyncExecutorVia4ArgConstructor() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, notifReg, exec, Executors.newSingleThreadExecutor());

        registry.registerHandler(CfRequest.class, new CfHandler());
        Response<String> resp = courier.send(new CfRequest());
        assertTrue(resp.isSuccess());
        assertEquals("future-ok", resp.getData());
    }

    @Test
    void courierUsesCustomTimeoutVia5ArgConstructor() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, notifReg, exec, null, 60_000);

        registry.registerHandler(CfRequest.class, new CfHandler());
        Response<String> resp = courier.send(new CfRequest());
        assertTrue(resp.isSuccess());
    }

    @Test
    void courierFallsBackToDefaultTimeoutWhenNegativeValueProvided() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        // Should not throw; falls back to 30_000
        Courier courier = new Courier(registry, notifReg, exec, null, -1);

        registry.registerHandler(CfRequest.class, new CfHandler());
        Response<String> resp = courier.send(new CfRequest());
        assertTrue(resp.isSuccess());
    }

    @Test
    void courierFallsBackToDefaultTimeoutWhenZeroProvided() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, notifReg, exec, null, 0);

        registry.registerHandler(CfRequest.class, new CfHandler());
        Response<String> resp = courier.send(new CfRequest());
        assertTrue(resp.isSuccess());
    }

    @Test
    void courierTimesOutWithCustomShortTimeout() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        // 1ms timeout — the slow handler will certainly exceed it
        Courier courier = new Courier(registry, notifReg, exec, null, 1);

        registry.registerHandler(SlowRequest.class, new SlowHandler());
        Response<String> resp = courier.send(new SlowRequest());

        assertFalse(resp.isSuccess());
        assertTrue(resp.getError().contains("timed out"));
        assertTrue(resp.getError().contains("1ms"));
    }

    @Test
    void courierTimesOutSynchronousHandlerThatBlocks() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        // 50ms timeout — the blocking sync handler sleeps for 10s
        Courier courier = new Courier(registry, notifReg, exec, null, 50);

        registry.registerHandler(SyncSlowRequest.class, new SyncSlowHandler());
        Response<String> resp = courier.send(new SyncSlowRequest());

        assertFalse(resp.isSuccess());
        assertEquals(504, resp.getStatusCode());
        assertTrue(resp.getError().contains("timed out"));
        assertTrue(resp.getError().contains("50ms"));
    }

    static class SyncSlowRequest implements IRequest<String> {}
    @SuppressWarnings("unused")
    static class SyncSlowHandler {
        public String handle(SyncSlowRequest r) {
            try { Thread.sleep(10_000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "too-late";
        }
    }

    static class SlowRequest implements IRequest<String> {}
    static class SlowHandler {
        public CompletableFuture<String> handle(SlowRequest r) {
            return CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(5_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "too-late";
            });
        }
    }

    // --- Coverage: inner CompletableFuture timeout (handler returns slow CF) ---

    @Test
    void courierTimesOutWhenReturnedCompletableFutureExceedsRemainingBudget() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        // 100ms timeout — handler returns instantly but CF takes 10s
        Courier courier = new Courier(registry, notifReg, exec, null, 100);

        registry.registerHandler(SlowCfRequest.class, new SlowCfInnerHandler());
        Response<String> resp = courier.send(new SlowCfRequest());

        assertFalse(resp.isSuccess());
        assertEquals(504, resp.getStatusCode());
        assertTrue(resp.getError().contains("timed out"));
    }

    @Test
    void courierReturnsErrorWhenReturnedCompletableFutureFailsWithException() {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(FailInnerCfRequest.class, new FailInnerCfHandler());

        Response<String> resp = fixture.courier().send(new FailInnerCfRequest());

        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
        assertEquals("IllegalStateException", resp.getExceptionType());
    }

    static class SlowCfRequest implements IRequest<String> {}
    @SuppressWarnings("unused")
    static class SlowCfInnerHandler {
        public CompletableFuture<String> handle(SlowCfRequest r) {
            // Returns a future that completes after 10s — will exceed timeout budget
            return CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(10_000); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "too-late";
            });
        }
    }

    static class FailInnerCfRequest implements IRequest<String> {}
    @SuppressWarnings("unused")
    static class FailInnerCfHandler {
        public CompletableFuture<String> handle(FailInnerCfRequest r) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("inner failure"));
            return future;
        }
    }
}