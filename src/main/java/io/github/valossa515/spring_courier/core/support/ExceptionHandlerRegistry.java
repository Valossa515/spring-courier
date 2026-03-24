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
 */
public class ExceptionHandlerRegistry {
    private static final Logger logger =
            LoggerFactory.getLogger(ExceptionHandlerRegistry.class);
    private final Map<Class<?>, List<IRequestExceptionHandler<?, ?, ?>>> handlers =
            new ConcurrentHashMap<>();
    private final List<IRequestExceptionHandler<?, ?, ?>> globalHandlers =
            new CopyOnWriteArrayList<>();
    private volatile boolean frozen = false;

    public void register(Class<?> requestType,
                         IRequestExceptionHandler<?, ?, ?> handler) {
        Objects.requireNonNull(requestType, "requestType must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        if (frozen) {
            throw new IllegalStateException(
                    "ExceptionHandlerRegistry is frozen; cannot register handler for: "
                            + requestType.getSimpleName());
        }
        if (requestType.isInterface()) {
            globalHandlers.add(handler);
            logger.info("Global exception handler registered: {}",
                    handler.getClass().getSimpleName());
        } else {
            handlers.computeIfAbsent(requestType,
                    k -> new CopyOnWriteArrayList<>()).add(handler);
            logger.info("Exception handler registered for {}: {}",
                    requestType.getSimpleName(),
                    handler.getClass().getSimpleName());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<IRequestExceptionHandler<?, ?, ?>> getHandlers(
            Class<?> requestType) {
        List<IRequestExceptionHandler<?, ?, ?>> result =
                new java.util.ArrayList<>();
        result.addAll(globalHandlers);
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
                + handlers.values().stream().mapToInt(List::size).sum();
        logger.info("ExceptionHandlerRegistry frozen with {} handlers", total);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int getCount() {
        return globalHandlers.size()
                + handlers.values().stream().mapToInt(List::size).sum();
    }
}
