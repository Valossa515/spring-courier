package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Contract for handlers that process notification/event messages.
 * Multiple handlers can be registered for the same notification type,
 * and all will be executed when the notification is published.
 *
 * @param <N> notification type to handle
 */
@FunctionalInterface
public interface NotificationHandler<N extends INotification> {

    /**
     * Handles the notification/event.
     *
     * @param notification the notification to handle
     */
    void handle(N notification);
}