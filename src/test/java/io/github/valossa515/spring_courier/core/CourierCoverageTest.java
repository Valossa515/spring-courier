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
}
