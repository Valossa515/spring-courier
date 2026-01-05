package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.exceptions.HandlerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry that maps request types to their handlers for use inside
 * the courier pipeline.
 */
public class HandlerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(HandlerRegistry.class);
    private final Map<Class<?>, Object> handlers = new ConcurrentHashMap<>();

    public void registerHandler(Class<?> requestType, Object handler) {
        if (handlers.containsKey(requestType)) {
            logger.warn("Replacing existing handler for request type: {}", requestType.getSimpleName());
        }
        handlers.put(requestType, handler);
        logger.debug("Handler registered for: {}", requestType.getSimpleName());
    }

    public <T> Object getHandler(Class<T> requestType) {
        Object handler = handlers.get(requestType);
        if (handler == null) {
            String errorMsg = "No handler registered for request type: " + requestType.getName();
            logger.error(errorMsg);
            throw new HandlerNotFoundException(errorMsg);
        }
        logger.debug("Handler found for: {}", requestType.getSimpleName());
        return handler;
    }

    public boolean hasHandlerFor(Class<?> requestType) {
        return handlers.containsKey(requestType);
    }

    public int getHandlerCount() {
        return handlers.size();
    }

    public Map<Class<?>, Object> getHandlers() {
        return new ConcurrentHashMap<>(handlers);
    }
}
