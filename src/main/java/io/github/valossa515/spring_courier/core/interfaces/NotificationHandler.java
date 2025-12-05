package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Contract for handlers that process notification/event messages.
 * Multiple handlers can be registered for the same notification type,
 * and all will be executed when the notification is published.
 *
 * @param <TNotification> notification type to handle
 */
public interface NotificationHandler<TNotification extends INotification> {
    
    /**
     * Handles the notification/event.
     *
     * @param notification the notification to handle
     */
    void handle(TNotification notification);
}
