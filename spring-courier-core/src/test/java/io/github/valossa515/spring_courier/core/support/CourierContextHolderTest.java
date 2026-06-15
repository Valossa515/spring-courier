package io.github.valossa515.spring_courier.core.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CourierContextHolderTest {

    @AfterEach
    void cleanup() {
        CourierContextHolder.clear();
    }

    @Test
    void getContextCreatesOneIfAbsent() {
        CourierContext ctx = CourierContextHolder.getContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getCorrelationId());
    }

    @Test
    void getContextReturnsSameInstance() {
        CourierContext first = CourierContextHolder.getContext();
        CourierContext second = CourierContextHolder.getContext();
        assertSame(first, second);
    }

    @Test
    void setAndGetContext() {
        CourierContext custom = CourierContext.create("custom-id");
        CourierContextHolder.setContext(custom);

        CourierContext retrieved = CourierContextHolder.getContext();
        assertEquals("custom-id", retrieved.getCorrelationId());
        assertSame(custom, retrieved);
    }

    @Test
    void clearRemovesContext() {
        CourierContextHolder.getContext();
        CourierContextHolder.clear();
        assertNull(CourierContextHolder.peekContext());
    }

    @Test
    void peekReturnsNullWhenNoneSet() {
        assertNull(CourierContextHolder.peekContext());
    }

    @Test
    void peekReturnsContextWhenSet() {
        CourierContext ctx = CourierContext.create("peek-test");
        CourierContextHolder.setContext(ctx);
        assertSame(ctx, CourierContextHolder.peekContext());
    }
}
