package io.github.valossa515.spring_courier.core.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRegistryTest {

    @Test
    void hasHandlersForReflectsRegistrations() {
        NotificationRegistry registry = new NotificationRegistry();

        assertFalse(registry.hasHandlersFor(String.class));

        Object handler = new Object();
        registry.registerHandler(String.class, handler);

        assertTrue(registry.hasHandlersFor(String.class));
    }

    @Test
    void hasHandlersForReturnsFalseForDifferentType() {
        NotificationRegistry registry = new NotificationRegistry();
        registry.registerHandler(Integer.class, new Object());

        assertFalse(registry.hasHandlersFor(String.class));
    }

    @Test
    void getHandlersReturnsDefensiveCopy() {
        NotificationRegistry registry = new NotificationRegistry();
        Object handler = new Object();
        registry.registerHandler(Integer.class, handler);

        List<Object> handlers = registry.getHandlers(Integer.class);
        assertEquals(1, handlers.size());
        assertSame(handler, handlers.get(0));

        handlers.clear();

        List<Object> handlersAgain = registry.getHandlers(Integer.class);
        assertEquals(1, handlersAgain.size());
        assertSame(handler, handlersAgain.get(0));
    }
}