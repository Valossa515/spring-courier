package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierBatchDispatchTest {

    private Courier courier;

    record EchoCmd(String msg) implements ICommand<String> {
    }

    record EchoQuery(String msg) implements IQuery<String> {
    }

    static class EchoCmdHandler
            implements CommandHandler<EchoCmd, String> {
        @Override
        public String handle(EchoCmd command) {
            return "cmd:" + command.msg();
        }
    }

    static class EchoQueryHandler
            implements QueryHandler<EchoQuery, String> {
        @Override
        public String handle(EchoQuery query) {
            return "qry:" + query.msg();
        }
    }

    @BeforeEach
    void setUp() {
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        NotificationRegistry notificationRegistry =
                new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        PipelineExecutor pipelineExecutor =
                new PipelineExecutor(pipelineRegistry);

        handlerRegistry.registerHandler(
                EchoCmd.class, new EchoCmdHandler());
        handlerRegistry.registerHandler(
                EchoQuery.class, new EchoQueryHandler());

        courier = new Courier(handlerRegistry, notificationRegistry,
                pipelineExecutor, new ProcessorRegistry(),
                null, 30_000,
                NotificationPublishingStrategy.SEQUENTIAL);
    }

    @AfterEach
    void cleanup() {
        Courier.clearMethodCaches();
    }

    @Test
    void sendAllDispatchesMultipleRequests() {
        List<? extends IRequest<?>> requests = List.of(
                new EchoCmd("a"),
                new EchoQuery("b"),
                new EchoCmd("c"));

        List<Response<?>> results = courier.sendAll(requests);

        assertEquals(3, results.size());
        assertEquals("cmd:a", results.get(0).getData());
        assertEquals("qry:b", results.get(1).getData());
        assertEquals("cmd:c", results.get(2).getData());
        assertTrue(results.stream().allMatch(Response::isSuccess));
    }

    @Test
    void sendAllWithEmptyListReturnsEmpty() {
        List<Response<?>> results = courier.sendAll(List.of());
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void sendAllAsyncDispatchesInParallel() throws Exception {
        List<? extends IRequest<?>> requests = List.of(
                new EchoCmd("x"),
                new EchoQuery("y"));

        CompletableFuture<List<Response<?>>> future =
                courier.sendAllAsync(requests);
        List<Response<?>> results = future.get();

        assertEquals(2, results.size());
        assertEquals("cmd:x", results.get(0).getData());
        assertEquals("qry:y", results.get(1).getData());
    }

    @Test
    void sendAllRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> courier.sendAll(null));
    }

    @Test
    void sendAllAsyncRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> courier.sendAllAsync(null));
    }
}
