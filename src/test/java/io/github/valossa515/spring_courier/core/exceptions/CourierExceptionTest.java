package io.github.valossa515.spring_courier.core.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CourierExceptionTest {

    @Test
    void capturesMessage() {
        CourierException ex = new CourierException("oops");
        assertEquals("oops", ex.getMessage());
    }

    @Test
    void capturesMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("root");
        CourierException ex = new CourierException("wrap", cause);

        assertEquals("wrap", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}

