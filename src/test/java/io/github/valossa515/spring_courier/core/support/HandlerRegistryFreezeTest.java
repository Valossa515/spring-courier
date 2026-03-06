package io.github.valossa515.spring_courier.core.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HandlerRegistryFreezeTest {

    @Test
    void freezePreventsNewRegistrations() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.registerHandler(String.class, new Object());

        assertFalse(registry.isFrozen());
        registry.freeze();
        assertTrue(registry.isFrozen());

        assertThrows(IllegalStateException.class,
                () -> registry.registerHandler(Integer.class, new Object()));
    }

    @Test
    void registerHandlerRejectsNullRequestType() {
        HandlerRegistry registry = new HandlerRegistry();
        assertThrows(NullPointerException.class,
                () -> registry.registerHandler(null, new Object()));
    }

    @Test
    void registerHandlerRejectsNullHandler() {
        HandlerRegistry registry = new HandlerRegistry();
        assertThrows(NullPointerException.class,
                () -> registry.registerHandler(String.class, null));
    }
}
