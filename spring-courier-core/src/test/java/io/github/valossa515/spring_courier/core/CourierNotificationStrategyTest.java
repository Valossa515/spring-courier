package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationHandlerException;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierNotificationStrategyTest {

    private Courier createCourier(NotificationPublishingStrategy strategy,
            NotificationRegistry notificationRegistry) {
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        PipelineExecutor pipelineExecutor =
                new PipelineExecutor(pipelineRegistry);
        return new Courier(handlerRegistry, notificationRegistry,
                pipelineExecutor, null, null, 30_000, strategy);
    }

    @Test
    void sequentialExecutesInOrder() {
        NotificationRegistry registry = new NotificationRegistry();
        List<String> order = new ArrayList<>();
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "A"));
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "B"));
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "C"));

        Courier courier = createCourier(
                NotificationPublishingStrategy.SEQUENTIAL, registry);
        courier.publish(new TestEvent());

        assertEquals(List.of("A", "B", "C"), order);
    }

    @Test
    void sequentialContinuesOnError() {
        NotificationRegistry registry = new NotificationRegistry();
        List<String> order = new ArrayList<>();
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "A"));
        registry.registerHandler(TestEvent.class,
                new FailingNotificationHandler());
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "C"));

        Courier courier = createCourier(
                NotificationPublishingStrategy.SEQUENTIAL, registry);
        courier.publish(new TestEvent());

        assertEquals(List.of("A", "C"), order);
    }

    @Test
    void parallelWhenAllExecutesAllHandlers() {
        NotificationRegistry registry = new NotificationRegistry();
        List<String> order = new CopyOnWriteArrayList<>();
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "A"));
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "B"));
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "C"));

        Courier courier = createCourier(
                NotificationPublishingStrategy.PARALLEL_WHEN_ALL,
                registry);
        courier.publish(new TestEvent());

        assertEquals(3, order.size());
        assertTrue(order.contains("A"));
        assertTrue(order.contains("B"));
        assertTrue(order.contains("C"));
    }

    @Test
    void stopOnFirstErrorStopsAfterFailure() {
        NotificationRegistry registry = new NotificationRegistry();
        List<String> order = new ArrayList<>();
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "A"));
        registry.registerHandler(TestEvent.class,
                new FailingNotificationHandler());
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "C"));

        Courier courier = createCourier(
                NotificationPublishingStrategy.STOP_ON_FIRST_ERROR,
                registry);

        assertThrows(NotificationHandlerException.class,
                () -> courier.publish(new TestEvent()));
        assertEquals(List.of("A"), order);
    }

    @Test
    void stopOnFirstErrorSucceedsWhenNoErrors() {
        NotificationRegistry registry = new NotificationRegistry();
        List<String> order = new ArrayList<>();
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "A"));
        registry.registerHandler(TestEvent.class,
                new OrderTrackingHandler(order, "B"));

        Courier courier = createCourier(
                NotificationPublishingStrategy.STOP_ON_FIRST_ERROR,
                registry);
        courier.publish(new TestEvent());

        assertEquals(List.of("A", "B"), order);
    }

    static class TestEvent implements INotification {
    }

    static class OrderTrackingHandler
            implements NotificationHandler<TestEvent> {
        private final List<String> order;
        private final String id;

        OrderTrackingHandler(List<String> order, String id) {
            this.order = order;
            this.id = id;
        }

        @Override
        public void handle(TestEvent notification) {
            order.add(id);
        }
    }

    static class FailingNotificationHandler
            implements NotificationHandler<TestEvent> {
        @Override
        public void handle(TestEvent notification) {
            throw new RuntimeException("boom");
        }
    }
}
