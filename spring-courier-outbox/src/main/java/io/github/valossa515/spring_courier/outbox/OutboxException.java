package io.github.valossa515.spring_courier.outbox;

/**
 * Raised when an outbox operation (serialization, persistence or dispatch
 * wiring) fails in a way the caller should be aware of.
 */
public class OutboxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OutboxException(String message) {
        super(message);
    }

    public OutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
