package io.github.valossa515.spring_courier.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourierMetricsTest {

    private MeterRegistry registry;
    private CourierMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new CourierMetrics(registry);
    }

    @Test
    void constructorRejectsNullRegistry() {
        assertThrows(NullPointerException.class, () -> new CourierMetrics(null));
    }

    @Test
    void recordSuccessCreatesTimer() {
        metrics.recordSuccess("CreateUserCommand", 5_000_000L);

        Timer timer = registry.find("courier.request")
                .tag("type", "CreateUserCommand")
                .tag("outcome", "success")
                .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordErrorCreatesTimerAndCounter() {
        metrics.recordError("CreateUserCommand", 3_000_000L, "IllegalArgumentException");

        Timer timer = registry.find("courier.request")
                .tag("type", "CreateUserCommand")
                .tag("outcome", "error")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());

        Counter counter = registry.find("courier.request.error")
                .tag("type", "CreateUserCommand")
                .tag("exception", "IllegalArgumentException")
                .counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordErrorHandlesNullExceptionName() {
        metrics.recordError("TestCommand", 1_000_000L, null);

        Counter counter = registry.find("courier.request.error")
                .tag("exception", "unknown")
                .counter();
        assertNotNull(counter);
    }

    @Test
    void recordNotificationCreatesCounter() {
        metrics.recordNotification("UserCreatedEvent");

        Counter counter = registry.find("courier.notification")
                .tag("type", "UserCreatedEvent")
                .counter();

        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void multipleRecordsAccumulate() {
        metrics.recordSuccess("TestCommand", 1_000_000L);
        metrics.recordSuccess("TestCommand", 2_000_000L);
        metrics.recordSuccess("TestCommand", 3_000_000L);

        Timer timer = registry.find("courier.request")
                .tag("type", "TestCommand")
                .tag("outcome", "success")
                .timer();

        assertNotNull(timer);
        assertEquals(3, timer.count());
    }
}
