package io.github.valossa515.spring_courier.integration;

import jakarta.xml.bind.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationExceptionTest {

    @Test
    void buildsWithMessageAndCause() {
        Exception cause = new IllegalArgumentException("cause");
        ValidationException ex = new ValidationException("msg", cause);

        assertEquals("msg", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void buildsWithMessageOnly() {
        ValidationException ex = new ValidationException("only-msg");
        assertEquals("only-msg", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void buildsWithNullCauseAndMessage() {
        ValidationException ex = new ValidationException("msg", (String) null);
        assertEquals("msg", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void buildsWithDefaultCtrLikeMessageOnly() {
        // Cobre construtor já existente (mensagem), reforçando linhas de novo código
        ValidationException ex = new ValidationException("only");
        assertEquals("only", ex.getMessage());
        assertNull(ex.getCause());
    }
}
