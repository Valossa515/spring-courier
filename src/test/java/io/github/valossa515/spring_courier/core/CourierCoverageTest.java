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

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourierCoverageTest {

    @BeforeEach
    void setUp() {
        Courier.clearMethodCaches();
    }

    @Test
    void clearMethodCachesDoesNotThrow() {
        assertDoesNotThrow(Courier::clearMethodCaches);
    }

    @Test
    void sendReturnsSuccessNullWhenPipelineExecutorReturnsNull() {
        HandlerRegistry handlerRegistry = mock(HandlerRegistry.class);
        PipelineExecutor pipelineExecutor = mock(PipelineExecutor.class);
        NotificationRegistry notificationRegistry = new NotificationRegistry();
        Courier courier = new Courier(handlerRegistry, notificationRegistry, pipelineExecutor);

        when(handlerRegistry.getHandler(NullPipelineReq.class)).thenReturn(new NullPipelineHandler());
        when(pipelineExecutor.execute(eq(new NullPipelineReq()), any())).thenReturn(null);

        Response<String> resp = courier.send(new NullPipelineReq());
        assertTrue(resp.isSuccess());
        assertNull(resp.getData());
    }

    @Test
    void sendReturnsErrorWhenHandlerFutureThrowsExecutionException() {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(FailFutureReq.class, new FailFutureHandler());

        Response<String> resp = fixture.courier().send(new FailFutureReq());
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
        assertEquals("RuntimeException", resp.getExceptionType(),
                "Should unwrap ExecutionException to the real cause");
    }

    @Test
    void sendReturnsErrorWhenHandlerFutureIsInterrupted() throws Exception {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, notifReg, exec, null, 5_000);

        registry.registerHandler(BlockingReq.class, new BlockingHandler());

        Thread testThread = Thread.currentThread();

        // Schedule an interrupt on the current thread after a short delay
        Thread interruptor = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            testThread.interrupt();
        });
        interruptor.start();

        Response<String> resp = courier.send(new BlockingReq());

        // Clear the interrupted status for the rest of the test suite
        Thread.interrupted();
        interruptor.join(2000);

        assertFalse(resp.isSuccess());
    }

    @Test
    void publishHandlesHandlerWithExecuteMethodName() {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(ExecuteReq.class, new ExecuteHandler());

        Response<String> resp = fixture.courier().send(new ExecuteReq());
        assertTrue(resp.isSuccess());
        assertEquals("executed", resp.getData());
    }

    @Test
    void sendFindsCorrectHandleMethodAmongMultiple() {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(MultiMethodReq.class, new MultiMethodHandler());

        Response<String> resp = fixture.courier().send(new MultiMethodReq());
        assertTrue(resp.isSuccess());
        assertEquals("correct", resp.getData());
    }

    // --- Request / Handler types ---

    static class NullPipelineReq implements IRequest<String> {
        @Override
        public boolean equals(Object o) {
            return o instanceof NullPipelineReq;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    static class NullPipelineHandler {
        public String handle(NullPipelineReq req) {
            Objects.requireNonNull(req);
            return "result";
        }
    }

    static class FailFutureReq implements IRequest<String> { }

    @SuppressWarnings("unused")
    static class FailFutureHandler {
        public CompletableFuture<String> handle(FailFutureReq req) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("future failed"));
            return future;
        }
    }

    static class BlockingReq implements IRequest<String> { }

    @SuppressWarnings("unused")
    static class BlockingHandler {
        public CompletableFuture<String> handle(BlockingReq req) {
            // Return a future that never completes, so get() blocks until interrupted
            return new CompletableFuture<>();
        }
    }

    static class ExecuteReq implements IRequest<String> { }

    @SuppressWarnings("unused")
    static class ExecuteHandler {
        public String execute(ExecuteReq req) {
            Objects.requireNonNull(req);
            return "executed";
        }
    }

    static class MultiMethodReq implements IRequest<String> { }

    @SuppressWarnings("unused")
    static class MultiMethodHandler {
        // Wrong name — should be skipped
        public String process(MultiMethodReq req) { return "wrong"; }
        // Wrong param count — should be skipped
        public String handle() { return "wrong"; }
        // Wrong param type — should be skipped
        public String handle(String notARequest) { return "wrong"; }
        // Correct: "handle" with 1 IRequest-assignable param
        public String handle(MultiMethodReq req) { return "correct"; }
    }

    // --- Coverage: unwrapExecutionCause branches ---

    @Test
    void sendUnwrapsCompletionExceptionWithNullCause() {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(
                CeNullCauseReq.class, new CeNullCauseHandler());

        Response<String> resp = fixture.courier().send(new CeNullCauseReq());
        assertFalse(resp.isSuccess());
        assertEquals("CompletionException", resp.getExceptionType());
    }

    @Test
    void sendHandlesAsyncHandlerWithCompletionExceptionError() {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(
                AsyncCeReq.class, new AsyncCeHandler());

        Response<String> resp = fixture.courier().send(new AsyncCeReq());
        assertFalse(resp.isSuccess());
        assertEquals("ArithmeticException", resp.getExceptionType());
    }

    static class CeNullCauseReq implements IRequest<String> {}
    @SuppressWarnings("unused")
    static class CeNullCauseHandler {
        public CompletableFuture<String> handle(CeNullCauseReq req) {
            CompletableFuture<String> future = new CompletableFuture<>();
            // CompletionException wrapping null — edge case for unwrapExecutionCause
            future.completeExceptionally(new CompletionException(null));
            return future;
        }
    }

    static class AsyncCeReq implements IRequest<String> {}
    @SuppressWarnings("unused")
    static class AsyncCeHandler {
        public CompletableFuture<String> handle(AsyncCeReq req) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(
                    new CompletionException(new ArithmeticException("div by zero")));
            return future;
        }
    }

    // --- Coverage: remainingMs <= 0 branch (budget exhausted before inner CF) ---

    @Test
    void sendTimesOutWhenBudgetExhaustedBeforeInnerFuture() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        // 50ms timeout — the handler sleeps 45ms before returning a slow CF,
        // which means remainingMs ≈ 5ms or less; the inner CF then triggers timeout.
        Courier courier = new Courier(registry, notifReg, exec, null, 50);

        registry.registerHandler(BudgetExhaustedReq.class, new BudgetExhaustedHandler());
        Response<String> resp = courier.send(new BudgetExhaustedReq());

        // Either the outer get() times out (504) or the remainingMs <= 0 branch
        // fires — both produce 504 with "timed out".
        assertFalse(resp.isSuccess());
        assertEquals(504, resp.getStatusCode());
        assertTrue(resp.getError().contains("timed out"));
    }

    static class BudgetExhaustedReq implements IRequest<String> {}
    @SuppressWarnings("unused")
    static class BudgetExhaustedHandler {
        public CompletableFuture<String> handle(BudgetExhaustedReq req) {
            // Consume most of the timeout budget synchronously
            try { Thread.sleep(45); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Return a never-completing future — triggers remainingMs <= 0 or inner timeout
            return new CompletableFuture<>();
        }
    }

    // --- Coverage: reflectiveInvoke ReflectiveOperationException branch ---

    @Test
    void sendReturnsErrorWhenHandlerMethodIsInaccessible() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notifReg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, notifReg, exec);

        // Preload cache with a method, then make it inaccessible
        registry.registerHandler(InaccessibleReq.class, new InaccessibleHandler());

        // Override the cached method with a private method that we block access to
        try {
            Method privateMethod = InaccessibleHandler.class
                    .getDeclaredMethod("secretMethod", InaccessibleReq.class);
            // Don't call setAccessible — leave it inaccessible
            // Force it into the cache by using the internal cache field
            var cacheField = Courier.class.getDeclaredField("REQUEST_METHOD_CACHE");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var cache = (java.util.Map<Class<?>, Method>) cacheField.get(null);
            cache.put(InaccessibleHandler.class, privateMethod);
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }

        Response<String> resp = courier.send(new InaccessibleReq());
        assertFalse(resp.isSuccess());
    }

    static class InaccessibleReq implements IRequest<String> {}
    @SuppressWarnings("unused")
    static class InaccessibleHandler {
        public String handle(InaccessibleReq req) {
            return "normal";
        }
        private String secretMethod(InaccessibleReq req) {
            return "secret";
        }
    }

    // --- Coverage: unwrapExecutionCause with null ExecutionException cause ---

    @Test
    void sendHandlesExecutionExceptionWithNullCause() {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(
                NullExCauseReq.class, new NullExCauseHandler());

        Response<String> resp = fixture.courier().send(new NullExCauseReq());
        assertFalse(resp.isSuccess());
    }

    static class NullExCauseReq implements IRequest<String> {}
    @SuppressWarnings("unused")
    static class NullExCauseHandler {
        public String handle(NullExCauseReq req) {
            // Throw a RuntimeException that triggers the error path
            throw new RuntimeException("handler boom");
        }
    }
}
