package io.github.valossa515.spring_courier.core.exceptions;

/**
 * Exception thrown when no handler is registered to process a given request.
 * It allows consumers to detect incomplete pipeline configurations and react by
 * registering the missing handler or handling the absence accordingly.
 */
public class HandlerNotFoundException extends CourierException {

    /**
     * Creates the exception with a descriptive message about the problem.
     *
     * @param message description of why the handler was not found.
     */
    public HandlerNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates the exception while associating the original cause that led to the
     * missing handler.
     *
     * @param message description of why the handler was not found.
     * @param cause   root exception that originated the problem.
     */
    public HandlerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
