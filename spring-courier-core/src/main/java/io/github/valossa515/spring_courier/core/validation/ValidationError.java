package io.github.valossa515.spring_courier.core.validation;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a validation error with field and message.
 */
public record ValidationError(String field, String message) {

    @Override
    public @NotNull String toString() {
        return "ValidationError{" +
                "field='" + field + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
