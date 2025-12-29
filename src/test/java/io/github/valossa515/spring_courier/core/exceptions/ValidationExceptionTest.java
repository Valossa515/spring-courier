package io.github.valossa515.spring_courier.core.exceptions;

import io.github.valossa515.spring_courier.core.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationExceptionTest {

    @Test
    void buildsMessageFromErrorsWhenNoMessageProvided() {
        List<ValidationError> errors = List.of(new ValidationError("f", "m"), new ValidationError("g", "n"));
        ValidationException ex = new ValidationException(errors);

        assertTrue(ex.getMessage().contains("2 error(s)"));
        assertEquals(errors, ex.getErrors());
    }

    @Test
    void copiesAndExposesErrorsDefensively() {
        List<ValidationError> errors = List.of(new ValidationError("field", "message"));
        ValidationException ex = new ValidationException("custom", errors);

        assertEquals("custom", ex.getMessage());
        assertEquals(errors, ex.getErrors());

        // ensure immutability of returned list
        assertThrows(UnsupportedOperationException.class, () -> ex.getErrors().add(new ValidationError("x", "y")));
    }

    @Test
    void handlesNullErrorsListGracefully() {
        ValidationException ex = new ValidationException((List<ValidationError>) null);

        assertTrue(ex.getMessage().contains("0 error(s)"));
        assertTrue(ex.getErrors().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> ex.getErrors().add(new ValidationError("f", "m")));
    }
}