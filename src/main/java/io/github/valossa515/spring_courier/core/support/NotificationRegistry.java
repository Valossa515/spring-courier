package io.github.valossa515.spring_courier.core.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        handlers.computeIfAbsent(notificationType, k -> new ArrayList<>()).add(handler);
        logger.debug("Notification handler registrado para: {}", notificationType.getSimpleName());
    }

    /**
     * Gets all handlers for a notification type.
     *
     * @param notificationType the notification type
     * @return list of handlers (may be empty)
     */
    public List<Object> getHandlers(Class<?> notificationType) {
        return new ArrayList<>(handlers.getOrDefault(notificationType, new ArrayList<>()));
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

    /**
     * Returns the total number of notification handlers registered.
     *
     * @return total handler count across all notification types
     */
    public int getHandlerCount() {
        return handlers.values().stream().mapToInt(List::size).sum();
    }
}
