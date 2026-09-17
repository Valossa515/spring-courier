package io.github.valossa515.spring_courier.messaging;

/**
 * Raised when an outbox message could not be handed to the broker.
 *
 * <p>Propagating instead of swallowing is deliberate: the poller records the
 * failed attempt and leaves the message pending, so it is retried later rather
 * than silently marked delivered.
 */
public class OutboxPublishException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OutboxPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
