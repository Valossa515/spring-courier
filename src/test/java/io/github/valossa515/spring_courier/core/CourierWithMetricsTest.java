package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierWithMetricsTest {

    private MeterRegistry registry;
    private HandlerRegistry handlerRegistry;
    private NotificationRegistry notificationRegistry;
    private Courier courier;

    @BeforeEach
    void setUp() {
        Courier.clearMethodCaches();
        registry = new SimpleMeterRegistry();
        CourierMetrics metrics = new CourierMetrics(registry);
        handlerRegistry = new HandlerRegistry();
        notificationRegistry = new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        PipelineExecutor pipelineExecutor =
                new PipelineExecutor(pipelineRegistry);
        courier = new Courier(handlerRegistry, notificationRegistry,
                pipelineExecutor, null, 30_000, metrics);
    }

    @Test
    void sendRecordsSuccessMetrics() {
        handlerRegistry.registerHandler(
                TestRequest.class, new TestHandler());

        Response<String> response = courier.send(new TestRequest());

        assertTrue(response.isSuccess());
        assertEquals("ok", response.getData());

        Timer timer = registry.find("courier.request")
                .tag("request_type", "TestRequest")
                .tag("status", "success")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void sendRecordsErrorMetrics() {
        handlerRegistry.registerHandler(
                TestRequest.class, new FailingHandler());

        Response<String> response = courier.send(new TestRequest());

        assertTrue(!response.isSuccess());

        Timer timer = registry.find("courier.request")
                .tag("request_type", "TestRequest")
                .tag("status", "error")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void sendTracksActiveRequests() {
        handlerRegistry.registerHandler(
                TestRequest.class, new TestHandler());

        assertEquals(0.0, registry.find("courier.request.active")
                .gauge().value());

        courier.send(new TestRequest());

        assertEquals(0.0, registry.find("courier.request.active")
                .gauge().value());
    }

    @Test
    void publishRecordsNotificationMetrics() {
        notificationRegistry.registerHandler(
                TestNotification.class, new TestNotificationHandler());

        courier.publish(new TestNotification());

        Timer timer = registry.find("courier.notification")
                .tag("notification_type", "TestNotification")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void multipleSendsAccumulateMetrics() {
        handlerRegistry.registerHandler(
                TestRequest.class, new TestHandler());

        courier.send(new TestRequest());
        courier.send(new TestRequest());
        courier.send(new TestRequest());

        Timer timer = registry.find("courier.request")
                .tag("request_type", "TestRequest")
                .tag("status", "success")
                .timer();
        assertNotNull(timer);
        assertEquals(3, timer.count());
    }

    static class TestRequest implements IRequest<String> {
    }

    @SuppressWarnings("unused")
    static class TestHandler {
        public String handle(TestRequest request) {
            Objects.requireNonNull(request);
            return "ok";
        }
    }

    @SuppressWarnings("unused")
    static class FailingHandler {
        public String handle(TestRequest request) {
            Objects.requireNonNull(request);
            throw new IllegalStateException("boom");
        }
    }

    static class TestNotification implements INotification {
    }

    @SuppressWarnings("unused")
    static class TestNotificationHandler {
        public void handle(TestNotification notification) {
            Objects.requireNonNull(notification);
        }
    }
}
