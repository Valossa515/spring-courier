package io.github.valossa515.spring_courier.core;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierMetricsTest {

    private MeterRegistry registry;
    private CourierMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new CourierMetrics(registry);
    }

    @Test
    void recordRequestCreatesTimerWithTags() {
        metrics.recordRequest("CreateOrder", true, 5_000_000L);

        Timer timer = registry.find("courier.request")
                .tag("request_type", "CreateOrder")
                .tag("status", "success")
                .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) >= 5_000_000L);
    }

    @Test
    void recordRequestTracksErrorStatus() {
        metrics.recordRequest("CreateOrder", false, 1_000_000L);

        Timer timer = registry.find("courier.request")
                .tag("request_type", "CreateOrder")
                .tag("status", "error")
                .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordNotificationCreatesTimerWithTag() {
        metrics.recordNotification("OrderCreated", 3_000_000L);

        Timer timer = registry.find("courier.notification")
                .tag("notification_type", "OrderCreated")
                .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordTimeoutIncrementsCounter() {
        metrics.recordTimeout("SlowRequest");
        metrics.recordTimeout("SlowRequest");

        double count = registry.find("courier.handler.timeout")
                .tag("request_type", "SlowRequest")
                .counter()
                .count();

        assertEquals(2.0, count);
    }

    @Test
    void recordErrorIncrementsCounter() {
        metrics.recordError("FailingRequest");

        double count = registry.find("courier.handler.error")
                .tag("request_type", "FailingRequest")
                .counter()
                .count();

        assertEquals(1.0, count);
    }

    @Test
    void activeRequestsGaugeTracksInFlight() {
        assertEquals(0.0, registry.find("courier.request.active")
                .gauge().value());

        metrics.incrementActive();
        assertEquals(1.0, registry.find("courier.request.active")
                .gauge().value());

        metrics.incrementActive();
        assertEquals(2.0, registry.find("courier.request.active")
                .gauge().value());

        metrics.decrementActive();
        assertEquals(1.0, registry.find("courier.request.active")
                .gauge().value());

        metrics.decrementActive();
        assertEquals(0.0, registry.find("courier.request.active")
                .gauge().value());
    }

    @Test
    void getRegistryReturnsConfiguredRegistry() {
        assertEquals(registry, metrics.getRegistry());
    }

    @Test
    void multipleRequestTypesAreTrackedIndependently() {
        metrics.recordRequest("TypeA", true, 1_000_000L);
        metrics.recordRequest("TypeB", true, 2_000_000L);
        metrics.recordRequest("TypeA", false, 3_000_000L);

        Timer timerASuccess = registry.find("courier.request")
                .tag("request_type", "TypeA")
                .tag("status", "success")
                .timer();
        Timer timerBSuccess = registry.find("courier.request")
                .tag("request_type", "TypeB")
                .tag("status", "success")
                .timer();
        Timer timerAError = registry.find("courier.request")
                .tag("request_type", "TypeA")
                .tag("status", "error")
                .timer();

        assertNotNull(timerASuccess);
        assertNotNull(timerBSuccess);
        assertNotNull(timerAError);
        assertEquals(1, timerASuccess.count());
        assertEquals(1, timerBSuccess.count());
        assertEquals(1, timerAError.count());
    }
}
