package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.exceptions.HandlerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandlerRegistryTest {

    private HandlerRegistry handlerRegistry;

    @BeforeEach
    void setUp() {
        handlerRegistry = new HandlerRegistry();
    }

    @Test
    void registerAndRetrieveHandlerSuccessfully() {
        DummyHandler handler = new DummyHandler();
        handlerRegistry.registerHandler(String.class, handler);

        Object registeredHandler = handlerRegistry.getHandler(String.class);

        assertSame(handler, registeredHandler);
        assertTrue(handlerRegistry.hasHandlerFor(String.class));
        assertEquals(1, handlerRegistry.getHandlerCount());
    }

    @Test
    void registerHandlerOverridesExistingHandler() {
        DummyHandler original = new DummyHandler();
        DummyHandler replacement = new DummyHandler();

        handlerRegistry.registerHandler(String.class, original);
        handlerRegistry.registerHandler(String.class, replacement);

        assertSame(replacement, handlerRegistry.getHandler(String.class));
        assertEquals(1, handlerRegistry.getHandlerCount(), "Registry should still report a single handler");
    }

    @Test
    void registerHandlerLogsAndOverridesExisting() {
        DummyHandler first = new DummyHandler();
        DummyHandler second = new DummyHandler();

        handlerRegistry.registerHandler(String.class, first);
        handlerRegistry.registerHandler(String.class, second);

        assertSame(second, handlerRegistry.getHandler(String.class));
    }

    @Test
    void getHandlerThrowsWhenHandlerMissing() {
        assertThrows(HandlerNotFoundException.class, () -> handlerRegistry.getHandler(Integer.class));
        assertFalse(handlerRegistry.hasHandlerFor(Integer.class));
    }

    @Test
    void getHandlersReturnsReadOnlyView() {
        DummyHandler handler = new DummyHandler();
        handlerRegistry.registerHandler(String.class, handler);

        Map<Class<?>, Object> exposedHandlers = handlerRegistry.getHandlers();
        assertThrows(UnsupportedOperationException.class, exposedHandlers::clear);
        assertEquals(1, handlerRegistry.getHandlerCount(), "Internal registry must remain unchanged");
        assertSame(handler, handlerRegistry.getHandler(String.class));
    }

    @Test
    void hasHandlerForReturnsFalseWhenNotRegistered() {
        assertFalse(handlerRegistry.hasHandlerFor(Double.class));
    }

    private static class DummyHandler {
    }
}
