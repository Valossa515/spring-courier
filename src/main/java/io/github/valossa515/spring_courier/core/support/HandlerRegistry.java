package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.exceptions.HandlerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry that maps request types to their handlers for use inside
 * the courier pipeline.
 */
public class HandlerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(HandlerRegistry.class);
    private final Map<Class<?>, Object> handlers = new ConcurrentHashMap<>();
    private volatile boolean frozen = false;

    public void registerHandler(Class<?> requestType, Object handler) {
        Objects.requireNonNull(requestType, "requestType must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        if (frozen) {
            throw new IllegalStateException(
                    "HandlerRegistry is frozen; cannot register handler for: "
                            + requestType.getSimpleName());
        }
        Object previous = handlers.put(requestType, handler);
        if (previous != null) {
            logger.warn("Replacing existing handler for request type: {}", requestType.getSimpleName());
        }
        logger.debug("Handler registered for: {}", requestType.getSimpleName());
    }

    /**
     * Freezes this registry, preventing any further handler registrations.
     * Should be called after application context startup is complete.
     */
    public void freeze() {
        this.frozen = true;
        logger.info("HandlerRegistry frozen with {} handlers", handlers.size());
    }

    public boolean isFrozen() {
        return frozen;
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
        return Collections.unmodifiableMap(handlers);
    }
}
