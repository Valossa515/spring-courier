package io.github.valossa515.spring_courier.core.metrics;

import io.github.valossa515.spring_courier.core.exceptions.ValidationException;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import io.github.valossa515.spring_courier.core.validation.ValidationError;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static io.github.valossa515.spring_courier.core.metrics.CourierMetrics.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeteredCourierTest {

    private HandlerRegistry handlerRegistry;
    private NotificationRegistry notificationRegistry;
    private PipelineRegistry pipelineRegistry;
    private PipelineExecutor pipelineExecutor;
    private SimpleMeterRegistry meterRegistry;
    private MeteredCourier courier;

    @BeforeEach
    void setUp() {
        handlerRegistry = new HandlerRegistry();
        notificationRegistry = new NotificationRegistry();
        pipelineRegistry = new PipelineRegistry();
        pipelineExecutor = new PipelineExecutor(pipelineRegistry);
        meterRegistry = new SimpleMeterRegistry();
        courier = new MeteredCourier(
                handlerRegistry, notificationRegistry,
                pipelineExecutor, pipelineRegistry,
                null, 30_000, meterRegistry);
    }

    @Test
    void sendRecordsTimerAndCounterOnSuccess() {
        handlerRegistry.registerHandler(TestCommand.class, new TestCommandHandler());
        Response<String> response = courier.send(new TestCommand());

        assertTrue(response.isSuccess());
        assertEquals("ok", response.getData());

        Timer timer = meterRegistry.find(SEND_DURATION)
                .tag(TAG_REQUEST_TYPE, "TestCommand")
                .tag(TAG_OUTCOME, OUTCOME_SUCCESS)
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());

        double count = meterRegistry.find(SEND_TOTAL)
                .tag(TAG_REQUEST_TYPE, "TestCommand")
                .tag(TAG_OUTCOME, OUTCOME_SUCCESS)
                .counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void sendTagsCommandCategory() {
        handlerRegistry.registerHandler(TestCommand.class, new TestCommandHandler());
        courier.send(new TestCommand());

        Timer timer = meterRegistry.find(SEND_DURATION)
                .tag(TAG_REQUEST_CATEGORY, CATEGORY_COMMAND)
                .timer();
        assertNotNull(timer);
    }

    @Test
    void sendTagsQueryCategory() {
        handlerRegistry.registerHandler(TestQuery.class, new TestQueryHandler());
        courier.send(new TestQuery());

        Timer timer = meterRegistry.find(SEND_DURATION)
                .tag(TAG_REQUEST_CATEGORY, CATEGORY_QUERY)
                .timer();
        assertNotNull(timer);
    }

    @Test
    void sendRecordsErrorCounterWhenHandlerFails() {
        handlerRegistry.registerHandler(TestCommand.class, new FailingHandler());
        Response<String> response = courier.send(new TestCommand());

        assertFalse(response.isSuccess());

        double errors = meterRegistry.find(HANDLER_ERRORS)
                .tag(TAG_REQUEST_TYPE, "TestCommand")
                .counter().count();
        assertEquals(1.0, errors);

        double total = meterRegistry.find(SEND_TOTAL)
                .tag(TAG_OUTCOME, OUTCOME_ERROR)
                .counter().count();
        assertEquals(1.0, total);
    }

    @Test
    void sendRecordsValidationFailureCounter() {
        handlerRegistry.registerHandler(TestCommand.class, new TestCommandHandler());

        // Use a mock PipelineExecutor to throw ValidationException
        PipelineExecutor mockExecutor = mock(PipelineExecutor.class);
        when(mockExecutor.execute(any(), any()))
                .thenThrow(new ValidationException(List.of(
                        new ValidationError("field", "test validation error"))));

        MeteredCourier validatingCourier = new MeteredCourier(
                handlerRegistry, notificationRegistry,
                mockExecutor, pipelineRegistry,
                null, 30_000, meterRegistry);

        assertThrows(ValidationException.class,
                () -> validatingCourier.send(new TestCommand()));

        double count = meterRegistry.find(VALIDATION_FAILURES)
                .tag(TAG_REQUEST_TYPE, "TestCommand")
                .counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void publishRecordsTimerAndCounter() {
        courier.publish(new TestNotification());

        Timer timer = meterRegistry.find(PUBLISH_DURATION)
                .tag(TAG_NOTIFICATION_TYPE, "TestNotification")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());

        double count = meterRegistry.find(PUBLISH_TOTAL)
                .tag(TAG_NOTIFICATION_TYPE, "TestNotification")
                .counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void publishAsyncRecordsTimerAndCounter() {
        courier.publishAsync(new TestNotification()).join();

        Timer timer = meterRegistry.find(PUBLISH_ASYNC_DURATION)
                .tag(TAG_NOTIFICATION_TYPE, "TestNotification")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());

        double count = meterRegistry.find(PUBLISH_TOTAL)
                .tag(TAG_NOTIFICATION_TYPE, "TestNotification")
                .counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void registersHandlerGauges() {
        handlerRegistry.registerHandler(TestCommand.class, new TestCommandHandler());

        Double gaugeValue = meterRegistry.find(HANDLERS_REGISTERED).gauge().value();
        assertNotNull(gaugeValue);
        assertEquals(1.0, gaugeValue);
    }

    @Test
    void registersNotificationHandlerGauge() {
        Double gaugeValue = meterRegistry.find(NOTIFICATION_HANDLERS_REGISTERED)
                .gauge().value();
        assertNotNull(gaugeValue);
        assertEquals(0.0, gaugeValue);
    }

    @Test
    void registersPipelineBehaviorGauge() {
        Double gaugeValue = meterRegistry.find(PIPELINE_BEHAVIORS_REGISTERED)
                .gauge().value();
        assertNotNull(gaugeValue);
        assertEquals(0.0, gaugeValue);
    }

    @Test
    void multipleSendsAccumulateCounters() {
        handlerRegistry.registerHandler(TestCommand.class, new TestCommandHandler());
        courier.send(new TestCommand());
        courier.send(new TestCommand());
        courier.send(new TestCommand());

        Timer timer = meterRegistry.find(SEND_DURATION)
                .tag(TAG_REQUEST_TYPE, "TestCommand")
                .timer();
        assertNotNull(timer);
        assertEquals(3, timer.count());
    }

    // --- Test stubs ---

    static class TestCommand implements ICommand<String> {
    }

    static class TestQuery implements IQuery<Integer> {
    }

    static class TestNotification implements INotification {
    }

    @SuppressWarnings("unused")
    static class TestCommandHandler {
        public String handle(TestCommand cmd) {
            Objects.requireNonNull(cmd);
            return "ok";
        }
    }

    @SuppressWarnings("unused")
    static class TestQueryHandler {
        public Integer handle(TestQuery query) {
            Objects.requireNonNull(query);
            return 42;
        }
    }

    @SuppressWarnings("unused")
    static class FailingHandler {
        public String handle(TestCommand cmd) {
            throw new RuntimeException("boom");
        }
    }
}
