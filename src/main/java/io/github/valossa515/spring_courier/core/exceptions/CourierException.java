package io.github.valossa515.spring_courier.core.exceptions;

/**
 * Base exception for all Courier-related errors.
 */
public sealed class CourierException extends RuntimeException
        permits HandlerNotFoundException, ValidationException {

    public CourierException(String message) {
        super(message);
    }

    public CourierException(String message, Throwable cause) {
        super(message, cause);
    }
}