package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class CourierInvokeTest {

    private CourierTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = CourierTestFixture.create();
    }

    @Test
    void returnsErrorWhenHandlerMethodMissing() {
        fixture.handlerRegistry().registerHandler(DummyRequest.class, new Object());

        var response = fixture.courier().send(new DummyRequest());

        assertFalse(response.isSuccess());
        assertEquals("An internal error occurred", response.getError());
    }

    @Test
    void wrapsInvocationTargetExceptionFromHandler() {
        fixture.handlerRegistry().registerHandler(DummyRequest.class, new ThrowingHandler());

        var response = fixture.courier().send(new DummyRequest());

        assertFalse(response.isSuccess());
        assertEquals("An internal error occurred", response.getError());
    }

    @Test
    void wrapsRuntimeExceptionInReflectionSetup() throws Exception {
        var method = Courier.class.getDeclaredMethod("getCachedMethod", Class.class);
        method.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> method.invoke(fixture.courier(), NoHandleMethod.class));
        assertTrue(thrown.getCause() instanceof Courier.HandlerMethodNotFoundException);
    }

    @Test
    void wrapsRuntimeExceptionWhenHandlerSignatureMismatch() {
        fixture.handlerRegistry().registerHandler(DummyRequest.class, new MismatchedHandler());

        var response = fixture.courier().send(new DummyRequest());

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
