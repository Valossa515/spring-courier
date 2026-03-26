package io.github.valossa515.spring_courier.core.support;

/**
 * Defines how notification handlers are executed when a notification
 * is published via {@link io.github.valossa515.spring_courier.core.Courier#publish}.
 *
 * <p>Configurable via {@code spring.courier.notification-strategy} property.
 */
public enum NotificationPublishingStrategy {

    /**
     * Handlers are invoked one-by-one in registration order.
     * If a handler throws, the exception is logged and subsequent
     * handlers still execute (fire-and-forget semantics).
     * This is the default behavior.
     */
    SEQUENTIAL,

    /**
     * All handlers are submitted to virtual threads concurrently.
     * The method blocks until every handler has completed (or failed).
     * Exceptions are logged but do not prevent other handlers from
     * completing.
     */
    PARALLEL_WHEN_ALL,

    /**
     * Handlers are invoked sequentially in registration order.
     * If any handler throws, execution stops immediately and a
     * {@link NotificationHandlerException} is thrown, wrapping the
     * original exception and identifying the failing handler.
     */
    STOP_ON_FIRST_ERROR
}
