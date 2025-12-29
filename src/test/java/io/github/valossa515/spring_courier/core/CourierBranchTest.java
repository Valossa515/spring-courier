package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class CourierBranchTest {

    @Test
    void sendReturnsResponseWhenPipelineAlreadyReturnsResponse() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notificationRegistry = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, notificationRegistry, exec);

        registry.registerHandler(SimpleReq.class, new SimpleHandler());

        Response<String> resp = courier.send(new SimpleReq());
        assertTrue(resp.isSuccess());
        assertEquals("direct", resp.getData());
    }

    @Test
    void sendHandlesNullFromPipeline() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry notificationRegistry = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, notificationRegistry, exec);

        registry.registerHandler(NullReq.class, new NullRespHandler());

        Response<String> resp = courier.send(new NullReq());
        assertTrue(resp.isSuccess());
        assertNull(resp.getData());
    }

    static class SimpleReq implements IRequest<String> { }
    static class SimpleHandler {
        public Response<String> handle(SimpleReq req) {
            Objects.requireNonNull(req);
            return Response.success("direct");
        }
    }

    static class NullReq implements IRequest<String> { }
    static class NullRespHandler {
        public CompletableFuture<String> handle(NullReq req) {
            Objects.requireNonNull(req);
            return CompletableFuture.completedFuture(null);
        }
    }
}