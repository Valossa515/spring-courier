package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Marker interface for notification/event messages that can be published
 * to multiple handlers. Unlike commands and queries, notifications don't
 * expect a response and can have zero or more handlers.
 * 
 * <p>This is useful for implementing domain events and event-driven architectures.</p>
 *
 * @see NotificationHandler
 */
public interface INotification {
}
