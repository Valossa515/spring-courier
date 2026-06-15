package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierPublishTest {

    private CourierTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = CourierTestFixture.create();
    }

    @Test
    void publishExitsQuietlyWhenNoHandlers() {
        assertDoesNotThrow(() -> fixture.courier().publish(new TestNotification()));
    }

    @Test
    void publishContinuesWhenHandlerThrows() {
        AtomicInteger callCount = new AtomicInteger();
        fixture.notificationRegistry().registerHandler(TestNotification.class, new FailingNotificationHandler());
        fixture.notificationRegistry().registerHandler(TestNotification.class, new CountingNotificationHandler(callCount));

        fixture.courier().publish(new TestNotification());

        assertEquals(1, callCount.get(), "Second handler should still be invoked");
    }

    @Test
    void publishSwallowsHandlerMissingHandleMethod() {
        fixture.notificationRegistry().registerHandler(TestNotification.class, new WrongMethodHandler());

        assertDoesNotThrow(() -> fixture.courier().publish(new TestNotification()));
    }

    @Test
    void publishAsyncUsesProvidedExecutor() throws Exception {
        ExecutorService customExecutor = Executors.newSingleThreadExecutor();
        NotificationRegistry notifReg = new NotificationRegistry();
        Courier courier = new Courier(new HandlerRegistry(), notifReg,
                new PipelineExecutor(new PipelineRegistry()), customExecutor);

        AtomicInteger callCount = new AtomicInteger();
        notifReg.registerHandler(TestNotification.class, new CountingNotificationHandler(callCount));

        courier.publishAsync(new TestNotification()).get(5, TimeUnit.SECONDS);

        assertEquals(1, callCount.get());
        customExecutor.shutdown();
        assertTrue(customExecutor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void publishAsyncUsesDefaultExecutorWhenNoneProvided() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        fixture.notificationRegistry().registerHandler(TestNotification.class, new CountingNotificationHandler(callCount));

        fixture.courier().publishAsync(new TestNotification()).get(5, TimeUnit.SECONDS);

        assertEquals(1, callCount.get());
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
