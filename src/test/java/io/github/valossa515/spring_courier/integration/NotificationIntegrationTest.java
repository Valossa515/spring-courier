package io.github.valossa515.spring_courier.integration;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class NotificationIntegrationTest {

    private Courier courier;
    private NotificationRegistry notificationRegistry;

    @BeforeEach
    void setUp() {
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        notificationRegistry = new NotificationRegistry();
        PipelineExecutor pipelineExecutor = new PipelineExecutor(new PipelineRegistry());
        courier = new Courier(handlerRegistry, notificationRegistry, pipelineExecutor);
    }

    @Test
    void publishInvokesAllRegisteredNotificationHandlers() {
        TestNotification notification = new TestNotification("test-message");
        List<String> executedHandlers = java.util.Collections.synchronizedList(new ArrayList<>());

        TestNotificationHandler1 handler1 = new TestNotificationHandler1(executedHandlers);
        TestNotificationHandler2 handler2 = new TestNotificationHandler2(executedHandlers);

        notificationRegistry.registerHandler(TestNotification.class, handler1);
        notificationRegistry.registerHandler(TestNotification.class, handler2);

        courier.publish(notification);

        assertEquals(2, executedHandlers.size(), "Expected 2 handlers to execute");
        assertTrue(executedHandlers.contains("handler1"), "handler1 should be in executed list");
        assertTrue(executedHandlers.contains("handler2"), "handler2 should be in executed list");
    }

    @Test
    void publishAsyncExecutesHandlersAsynchronously() throws Exception {
        TestNotification notification = new TestNotification("async-test");
        List<String> executedHandlers = java.util.Collections.synchronizedList(new ArrayList<>());

        TestNotificationHandler1 handler1 = new TestNotificationHandler1(executedHandlers);
        notificationRegistry.registerHandler(TestNotification.class, handler1);

        CompletableFuture<Void> future = courier.publishAsync(notification);
        future.get(); // Wait for completion

        assertEquals(1, executedHandlers.size(), "Expected 1 handler to execute");
        assertTrue(executedHandlers.contains("handler1"), "handler1 should be in executed list");
    }

    @Test
    void publishHandlesNoRegisteredHandlers() {
        TestNotification notification = new TestNotification("no-handlers");
        
        // Should not throw exception
        assertDoesNotThrow(() -> courier.publish(notification));
    }

    @Test
    void notificationDiscoveryPostProcessorRegistersHandlers() {
        ApplicationContext context = mock(ApplicationContext.class);
        NotificationDiscoveryPostProcessor processor = 
            new NotificationDiscoveryPostProcessor(notificationRegistry, context);

        TestNotificationHandler1 handler = new TestNotificationHandler1(new ArrayList<>());
        processor.postProcessAfterInitialization(handler, "testHandler");

        assertTrue(notificationRegistry.hasHandlersFor(TestNotification.class));
    }

    // Test classes
        record TestNotification(String message) implements INotification {
    }

    static class TestNotificationHandler1 implements NotificationHandler<TestNotification> {
        private final List<String> executedHandlers;

        TestNotificationHandler1(List<String> executedHandlers) {
            this.executedHandlers = executedHandlers;
        }

        @Override
        public void handle(TestNotification notification) {
            executedHandlers.add("handler1");
        }
    }

    static class TestNotificationHandler2 implements NotificationHandler<TestNotification> {
        private final List<String> executedHandlers;

        TestNotificationHandler2(List<String> executedHandlers) {
            this.executedHandlers = executedHandlers;
        }

        @Override
        public void handle(TestNotification notification) {
            executedHandlers.add("handler2");
        }
    }
}
