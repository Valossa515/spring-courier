package io.github.valossa515.spring_courier.core.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry that maps notification types to their handlers.
 * Unlike command/query handlers, multiple handlers can be registered
 * for a single notification type.
 */
public class NotificationRegistry {
    private static final Logger logger = LoggerFactory.getLogger(NotificationRegistry.class);
    private final Map<Class<?>, List<Object>> handlers = new ConcurrentHashMap<>();

    /**
     * Registers a notification handler.
     *
     * @param notificationType the notification type
     * @param handler          the handler instance
     */
    public void registerHandler(Class<?> notificationType, Object handler) {
        Objects.requireNonNull(notificationType, "notificationType must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        handlers.computeIfAbsent(notificationType, k -> new CopyOnWriteArrayList<>()).add(handler);
        logger.debug("Notification handler registered for: {}", notificationType.getSimpleName());
    }

    /**
     * Gets all handlers for a notification type.
     *
     * @param notificationType the notification type
     * @return unmodifiable list of handlers (maybe empty)
     */
    public List<Object> getHandlers(Class<?> notificationType) {
        List<Object> handlerList = handlers.get(notificationType);
        if (handlerList == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(handlerList);
    }

    /**
     * Checks if any handlers are registered for the notification type.
     *
     * @param notificationType the notification type
     * @return true if at least one handler is registered
     */
    public boolean hasHandlersFor(Class<?> notificationType) {
        List<Object> handlerList = handlers.get(notificationType);
        return handlerList != null && !handlerList.isEmpty();
    }
}
