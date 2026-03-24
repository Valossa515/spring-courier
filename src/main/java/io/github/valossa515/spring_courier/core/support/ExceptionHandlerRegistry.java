package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry for {@link IRequestExceptionHandler} instances.
 * Global handlers are stored with their resolved request type so
 * that only type-compatible handlers are returned at lookup time.
 */
public class ExceptionHandlerRegistry {
    private static final Logger logger =
            LoggerFactory.getLogger(ExceptionHandlerRegistry.class);
    private final Map<Class<?>, List<IRequestExceptionHandler<?, ?, ?>>>
            handlers = new ConcurrentHashMap<>();
    private final List<GlobalEntry> globalHandlers =
            new CopyOnWriteArrayList<>();
    private volatile boolean frozen = false;

    record GlobalEntry(IRequestExceptionHandler<?, ?, ?> handler,
                       Class<?> requestType) {
    }

    public void register(Class<?> requestType,
                         IRequestExceptionHandler<?, ?, ?> handler) {
        Objects.requireNonNull(requestType, "requestType must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        if (frozen) {
            throw new IllegalStateException(
                    "ExceptionHandlerRegistry is frozen; cannot register "
                            + "handler for: "
                            + requestType.getSimpleName());
        }
        if (requestType.isInterface()) {
            globalHandlers.add(new GlobalEntry(handler, requestType));
            logger.info(
                    "Global exception handler registered: {} (matches {})",
                    handler.getClass().getSimpleName(),
                    requestType.getSimpleName());
        } else {
            handlers.computeIfAbsent(requestType,
                    k -> new CopyOnWriteArrayList<>()).add(handler);
            logger.info("Exception handler registered for {}: {}",
                    requestType.getSimpleName(),
                    handler.getClass().getSimpleName());
        }
    }

    public List<IRequestExceptionHandler<?, ?, ?>> getHandlers(
            Class<?> requestType) {
        List<IRequestExceptionHandler<?, ?, ?>> result =
                new java.util.ArrayList<>();
        for (GlobalEntry entry : globalHandlers) {
            if (entry.requestType() != null
                    && entry.requestType().isAssignableFrom(requestType)) {
                result.add(entry.handler());
            }
        }
        List<IRequestExceptionHandler<?, ?, ?>> specific =
                handlers.get(requestType);
        if (specific != null) {
            result.addAll(specific);
        }
        return Collections.unmodifiableList(result);
    }

    public void freeze() {
        this.frozen = true;
        int total = globalHandlers.size()
                + handlers.values().stream()
                .mapToInt(List::size).sum();
        logger.info(
                "ExceptionHandlerRegistry frozen with {} handlers", total);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int getCount() {
        return globalHandlers.size()
                + handlers.values().stream()
                .mapToInt(List::size).sum();
    }
}
