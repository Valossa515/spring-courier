package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class CourierBranchTest {

    private CourierTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = CourierTestFixture.create();
    }

    @Test
    void sendReturnsResponseWhenPipelineAlreadyReturnsResponse() {
        fixture.handlerRegistry().registerHandler(SimpleReq.class, new SimpleHandler());

        Response<String> resp = fixture.courier().send(new SimpleReq());
        assertTrue(resp.isSuccess());
        assertEquals("direct", resp.getData());
    }

    @Test
    void sendHandlesNullFromPipeline() {
        fixture.handlerRegistry().registerHandler(NullReq.class, new NullRespHandler());

        Response<String> resp = fixture.courier().send(new NullReq());
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