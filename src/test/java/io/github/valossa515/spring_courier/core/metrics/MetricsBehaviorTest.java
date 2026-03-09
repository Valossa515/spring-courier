package io.github.valossa515.spring_courier.core.metrics;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.support.Response;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsBehaviorTest {

    private MeterRegistry registry;
    private MetricsBehavior<TestCommand, Response<String>> behavior;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        CourierMetrics metrics = new CourierMetrics(registry);
        behavior = new MetricsBehavior<>(metrics);
    }

    @Test
    void recordsSuccessMetricOnSuccessfulResponse() {
        TestCommand command = new TestCommand();

        Response<String> result = behavior.handle(command,
                () -> Response.success("ok"));

        assertTrue(result.isSuccess());
        Timer timer = registry.find("courier.request")
                .tag("type", "TestCommand")
                .tag("outcome", "success")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordsErrorMetricOnErrorResponse() {
        TestCommand command = new TestCommand();

        Response<String> result = behavior.handle(command,
                () -> Response.error("fail", 400));

        assertEquals(false, result.isSuccess());
        Timer timer = registry.find("courier.request")
                .tag("type", "TestCommand")
                .tag("outcome", "error")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordsErrorMetricOnException() {
        TestCommand command = new TestCommand();

        assertThrows(RuntimeException.class, () ->
                behavior.handle(command, () -> {
                    throw new IllegalStateException("boom");
                })
        );

        Timer timer = registry.find("courier.request")
                .tag("type", "TestCommand")
                .tag("outcome", "error")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void returnsResultFromNextHandler() {
        TestCommand command = new TestCommand();
        Response<String> expected = Response.success("data");

        Response<String> result = behavior.handle(command, () -> expected);

        assertEquals("data", result.getData());
    }

    static class TestCommand implements ICommand<Response<String>> { }
}
