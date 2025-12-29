package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class CourierInvokeTest {

    @Test
    void returnsErrorWhenHandlerMethodMissing() {
        Courier courier = new Courier(new HandlerRegistry(), new NotificationRegistry(), new PipelineExecutor(new PipelineRegistry()));
        courier.handlerRegistry.registerHandler(DummyRequest.class, new Object());

        var response = courier.send(new DummyRequest());

        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("No handle method"));
    }

    @Test
    void wrapsInvocationTargetExceptionFromHandler() {
        HandlerRegistry registry = new HandlerRegistry();
        Courier courier = new Courier(registry, new NotificationRegistry(), new PipelineExecutor(new PipelineRegistry()));
        registry.registerHandler(DummyRequest.class, new ThrowingHandler());

        var response = courier.send(new DummyRequest());

        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("handler boom"));
    }

    @Test
    void wrapsRuntimeExceptionInReflectionSetup() throws Exception {
        Courier courier = new Courier(new HandlerRegistry(), new NotificationRegistry(), new PipelineExecutor(new PipelineRegistry()));
        var method = Courier.class.getDeclaredMethod("findHandleOrExecuteMethod", Class.class);
        method.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> method.invoke(courier, NoHandleMethod.class));
        assertTrue(thrown.getCause() instanceof Courier.HandlerMethodNotFoundException);
    }

    @Test
    void wrapsRuntimeExceptionWhenHandlerSignatureMismatch() {
        HandlerRegistry registry = new HandlerRegistry();
        Courier courier = new Courier(registry, new NotificationRegistry(), new PipelineExecutor(new PipelineRegistry()));
        registry.registerHandler(DummyRequest.class, new MismatchedHandler());

        var response = courier.send(new DummyRequest());

        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
    }

    private static class DummyRequest implements IRequest<String> { }

    @SuppressWarnings("unused")
    private static class ThrowingHandler {
        public String handle(DummyRequest request) {
            throw new IllegalStateException("handler boom");
        }
    }

    private static class NoHandleMethod { }

    private static class MismatchedHandler {
        public String handle(OtherRequest request) {
            return "ok";
        }
    }

    private static class OtherRequest implements IRequest<String> { }
}
