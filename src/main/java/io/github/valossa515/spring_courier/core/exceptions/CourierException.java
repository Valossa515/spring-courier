package io.github.valossa515.spring_courier.core.exceptions;

/**
 * Base exception for all Courier-related errors.
 */
public class CourierException extends RuntimeException {

    public CourierException(String message) {
        super(message);
    }

    public CourierException(String message, Throwable cause) {
        super(message, cause);
    }
}