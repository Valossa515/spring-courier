package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CourierAdditionalTest {
    @Test
    void sendHandlesCompletableFutureResult() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry reg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, reg, exec);

        registry.registerHandler(CfRequest.class, new CfHandler());

        Response<String> resp = courier.send(new CfRequest());
        assertTrue(resp.isSuccess());
        assertEquals("future-ok", resp.getData());
    }

    @Test
    void sendReturnsSuccessWithNullPayload() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry reg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, reg, exec);

        registry.registerHandler(NullRequest.class, new NullHandler());

        Response<String> resp = courier.send(new NullRequest());
        assertTrue(resp.isSuccess());
        assertFalse(resp.hasData());
        assertNull(resp.getData());
    }

    @Test
    void publishInvokesNonPublicHandlerWithAccessible() {
        HandlerRegistry registry = new HandlerRegistry();
        NotificationRegistry reg = new NotificationRegistry();
        PipelineExecutor exec = new PipelineExecutor(new PipelineRegistry());
        Courier courier = new Courier(registry, reg, exec);

        AtomicInteger counter = new AtomicInteger();
        reg.registerHandler(TestNotification.class, new PrivateNotificationHandler(counter));

        courier.publish(new TestNotification());
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
}