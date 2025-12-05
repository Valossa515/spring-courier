package io.github.valossa515.spring_courier.core.exceptions;

import io.github.valossa515.spring_courier.core.validation.ValidationError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when request validation fails.
 */
public class ValidationException extends RuntimeException {
    private final List<ValidationError> errors;

    public ValidationException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public ValidationException(List<ValidationError> errors) {
        super("Validation failed with " + (errors != null ? errors.size() : 0) + " error(s)");
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
