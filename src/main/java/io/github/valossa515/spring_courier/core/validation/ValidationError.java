package io.github.valossa515.spring_courier.core.validation;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a validation error with field and message.
 * Field and message values are sanitized to prevent log injection.
 */
public record ValidationError(String field, String message) {

    public ValidationError {
        field = field != null ? field : "unknown";
        message = message != null ? message : "validation failed";
    }

    @Override
    public @NotNull String toString() {
        return "ValidationError{" +
                "field='" + field + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
