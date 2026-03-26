package io.github.valossa515.spring_courier.core.support;

/**
 * Thrown when a notification handler fails under the
 * {@link NotificationPublishingStrategy#STOP_ON_FIRST_ERROR} strategy.
 */
public class NotificationHandlerException extends RuntimeException {

    private final String handlerName;

    public NotificationHandlerException(String handlerName, Throwable cause) {
        super("Notification handler failed: " + handlerName, cause);
        this.handlerName = handlerName;
    }

    public String getHandlerName() {
        return handlerName;
    }
}
