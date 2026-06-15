package io.github.valossa515.spring_courier.core.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRegistryFreezeTest {

    @Test
    void freezePreventsNewRegistrations() {
        NotificationRegistry registry = new NotificationRegistry();
        registry.registerHandler(String.class, new Object());

        assertFalse(registry.isFrozen());
        registry.freeze();
        assertTrue(registry.isFrozen());

        assertThrows(IllegalStateException.class,
                () -> registry.registerHandler(Integer.class, new Object()));
    }

    @Test
    void registerHandlerRejectsNullNotificationType() {
        NotificationRegistry registry = new NotificationRegistry();
        assertThrows(NullPointerException.class,
                () -> registry.registerHandler(null, new Object()));
    }

    @Test
    void registerHandlerRejectsNullHandler() {
        NotificationRegistry registry = new NotificationRegistry();
        assertThrows(NullPointerException.class,
                () -> registry.registerHandler(String.class, null));
    }

    @Test
    void getHandlersReturnsEmptyForUnregisteredType() {
        NotificationRegistry registry = new NotificationRegistry();
        List<Object> handlers = registry.getHandlers(String.class);
        assertTrue(handlers.isEmpty());
    }

    @Test
    void hasHandlersForReturnsFalseWhenListIsEmpty() {
        NotificationRegistry registry = new NotificationRegistry();
        assertFalse(registry.hasHandlersFor(String.class));
    }
}
