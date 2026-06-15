package io.github.valossa515.spring_courier.core.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class HandlerNotFoundExceptionTest {

    @Test
    void storesMessage() {
        HandlerNotFoundException ex = new HandlerNotFoundException("no handler");
        assertEquals("no handler", ex.getMessage());
    }

    @Test
    void storesMessageAndCause() {
        RuntimeException cause = new RuntimeException("missing");
        HandlerNotFoundException ex = new HandlerNotFoundException("problem", cause);

        assertEquals("problem", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}

