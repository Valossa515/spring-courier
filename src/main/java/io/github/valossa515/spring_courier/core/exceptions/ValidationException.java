package io.github.valossa515.spring_courier.core.exceptions;

import io.github.valossa515.spring_courier.core.validation.ValidationError;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when request validation fails.
 */
public final class ValidationException extends CourierException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<ValidationError> errors;

    private static List<ValidationError> copy(List<ValidationError> source) {
        return source != null ? new ArrayList<>(source) : new ArrayList<>();
    }

    public ValidationException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = copy(errors);
    }

    public ValidationException(List<ValidationError> errors) {
        this("Validation failed with " + (errors != null ? errors.size() : 0) + " error(s)", errors);
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}