package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CourierPublishTest {

    @Test
    void publishExitsQuietlyWhenNoHandlers() {
        NotificationRegistry notificationRegistry = new NotificationRegistry();
        Courier courier = new Courier(new HandlerRegistry(), notificationRegistry, new PipelineExecutor(new PipelineRegistry()));

        assertDoesNotThrow(() -> courier.publish(new TestNotification()));
    }

    @Test
    void publishContinuesWhenHandlerThrows() {
        NotificationRegistry notificationRegistry = new NotificationRegistry();
        Courier courier = new Courier(new HandlerRegistry(), notificationRegistry, new PipelineExecutor(new PipelineRegistry()));

        AtomicInteger callCount = new AtomicInteger();
        notificationRegistry.registerHandler(TestNotification.class, new FailingNotificationHandler());
        notificationRegistry.registerHandler(TestNotification.class, new CountingNotificationHandler(callCount));

        courier.publish(new TestNotification());

        assertEquals(1, callCount.get(), "Second handler should still be invoked");
    }

    @Test
    void publishSwallowsHandlerMissingHandleMethod() {
        NotificationRegistry notificationRegistry = new NotificationRegistry();
        Courier courier = new Courier(new HandlerRegistry(), notificationRegistry, new PipelineExecutor(new PipelineRegistry()));

        notificationRegistry.registerHandler(TestNotification.class, new WrongMethodHandler());

        assertDoesNotThrow(() -> courier.publish(new TestNotification()));
    }

    private static class TestNotification implements INotification { }

    private static class FailingNotificationHandler {
        public void handle(TestNotification notification) {
            throw new IllegalStateException("boom");
        }
    }

    private record CountingNotificationHandler(AtomicInteger counter) {

        public void handle(TestNotification notification) {
                counter.incrementAndGet();
            }
        }

    private static class WrongMethodHandler {
        // No handle method present
        public void process(TestNotification notification) { }
    }
}
