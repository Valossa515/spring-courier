package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.CourierContext;
import io.github.valossa515.spring_courier.core.support.CourierContextHolder;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CourierContextIntegrationTest {

    private HandlerRegistry handlerRegistry;
    private Courier courier;
    private final AtomicReference<String> capturedCorrelationId =
            new AtomicReference<>();

    record CtxCommand() implements ICommand<String> {
    }

    static class CtxHandler
            implements CommandHandler<CtxCommand, String> {
        private final AtomicReference<String> captured;

        CtxHandler(AtomicReference<String> captured) {
            this.captured = captured;
        }

        @Override
        public String handle(CtxCommand command) {
            CourierContext ctx = CourierContextHolder.getContext();
            captured.set(ctx.getCorrelationId());
            return ctx.getCorrelationId();
        }
    }

    @BeforeEach
    void setUp() {
        handlerRegistry = new HandlerRegistry();
        NotificationRegistry notificationRegistry =
                new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        PipelineExecutor pipelineExecutor =
                new PipelineExecutor(pipelineRegistry);

        handlerRegistry.registerHandler(CtxCommand.class,
                new CtxHandler(capturedCorrelationId));

        courier = new Courier(handlerRegistry, notificationRegistry,
                pipelineExecutor, new ProcessorRegistry(),
                null, 30_000,
                NotificationPublishingStrategy.SEQUENTIAL);
    }

    @AfterEach
    void cleanup() {
        CourierContextHolder.clear();
        Courier.clearMethodCaches();
    }

    @Test
    void sendCreatesContextAutomatically() {
        Response<String> response = courier.send(new CtxCommand());

        assertNotNull(capturedCorrelationId.get());
        assertEquals(capturedCorrelationId.get(),
                response.getData());
        // Context should be cleared after send
        assertNull(CourierContextHolder.peekContext());
    }

    @Test
    void sendPreservesExistingContext() {
        CourierContext preSet =
                CourierContext.create("pre-existing-id");
        CourierContextHolder.setContext(preSet);

        Response<String> response = courier.send(new CtxCommand());

        assertEquals("pre-existing-id", response.getData());
        // Context should NOT be cleared since we didn't create it
        assertNotNull(CourierContextHolder.peekContext());
        assertEquals("pre-existing-id",
                CourierContextHolder.getContext()
                        .getCorrelationId());
    }

    @Test
    void sendAsyncPropagatesContext() throws Exception {
        CourierContext preSet =
                CourierContext.create("async-ctx-id");
        CourierContextHolder.setContext(preSet);

        CompletableFuture<Response<String>> future =
                courier.sendAsync(new CtxCommand());
        Response<String> response = future.get();

        assertEquals("async-ctx-id", response.getData());
    }

    @Test
    void sendAsyncWithoutPreExistingContextCreatesOne()
            throws Exception {
        CompletableFuture<Response<String>> future =
                courier.sendAsync(new CtxCommand());
        Response<String> response = future.get();

        assertNotNull(response.getData());
    }
}
