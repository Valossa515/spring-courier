package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry for {@link IRequestExceptionHandler} instances.
 * Global handlers are stored with their resolved request type so
 * that only type-compatible handlers are returned at lookup time.
 * Each entry also stores the resolved exception type {@code E} so
 * that handlers are only invoked for matching exception classes.
 */
public class ExceptionHandlerRegistry {
    private static final Logger logger =
            LoggerFactory.getLogger(ExceptionHandlerRegistry.class);
    private final Map<Class<?>, List<HandlerEntry>> handlers =
            new ConcurrentHashMap<>();
    private final List<GlobalEntry> globalHandlers =
            new CopyOnWriteArrayList<>();
    private volatile boolean frozen = false;

    record HandlerEntry(IRequestExceptionHandler<?, ?, ?> handler,
                        Class<? extends Exception> exceptionType) {
    }

    record GlobalEntry(IRequestExceptionHandler<?, ?, ?> handler,
                       Class<?> requestType,
                       Class<? extends Exception> exceptionType) {
    }

    /**
     * Registers an exception handler with the default exception type
     * ({@link Exception}). Use
     * {@link #register(Class, IRequestExceptionHandler, Class)} to
     * provide an explicit exception type for filtering.
     */
    public void register(Class<?> requestType,
                         IRequestExceptionHandler<?, ?, ?> handler) {
        register(requestType, handler, Exception.class);
    }

    /**
     * Registers an exception handler with an explicit exception type
     * for filtering.
     */
    @SuppressWarnings("unchecked")
    public void register(Class<?> requestType,
                         IRequestExceptionHandler<?, ?, ?> handler,
                         Class<? extends Exception> exceptionType) {
        Objects.requireNonNull(requestType,
                "requestType must not be null");
        Objects.requireNonNull(handler,
                "handler must not be null");
        if (frozen) {
            throw new IllegalStateException(
                    "ExceptionHandlerRegistry is frozen; cannot "
                            + "register handler for: "
                            + requestType.getSimpleName());
        }
        Class<? extends Exception> exType = exceptionType != null
                ? exceptionType : Exception.class;
        if (requestType.isInterface()) {
            globalHandlers.add(
                    new GlobalEntry(handler, requestType, exType));
            globalHandlers.sort(Comparator.comparingInt(
                    e -> getHandlerOrder(e.handler())));
            logger.info(
                    "Global exception handler registered: {} "
                            + "(matches {}, exception {})",
                    handler.getClass().getSimpleName(),
                    requestType.getSimpleName(),
                    exType.getSimpleName());
        } else {
            List<HandlerEntry> list = handlers.computeIfAbsent(
                    requestType,
                    k -> new CopyOnWriteArrayList<>());
            list.add(new HandlerEntry(handler, exType));
            list.sort(Comparator.comparingInt(
                    e -> getHandlerOrder(e.handler())));
            logger.info("Exception handler registered for {}: {} "
                            + "(exception {})",
                    requestType.getSimpleName(),
                    handler.getClass().getSimpleName(),
                    exType.getSimpleName());
        }
    }

    /**
     * Returns all exception handlers applicable to the given request
     * type, regardless of exception type filtering.
     */
    public List<IRequestExceptionHandler<?, ?, ?>> getHandlers(
            Class<?> requestType) {
        List<IRequestExceptionHandler<?, ?, ?>> result =
                new ArrayList<>();
        for (GlobalEntry entry : globalHandlers) {
            if (entry.requestType() != null
                    && entry.requestType()
                    .isAssignableFrom(requestType)) {
                result.add(entry.handler());
            }
        }
        List<HandlerEntry> specific = handlers.get(requestType);
        if (specific != null) {
            for (HandlerEntry entry : specific) {
                result.add(entry.handler());
            }
        }
        result.sort(Comparator.comparingInt(
                this::getHandlerOrder));
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns exception handlers applicable to the given request type
     * that also match the provided exception class.
     *
     * @param requestType   the request class being dispatched
     * @param exceptionType the thrown exception class
     * @return filtered, order-sorted list of matching handlers
     */
    public List<IRequestExceptionHandler<?, ?, ?>> getHandlers(
            Class<?> requestType,
            Class<? extends Exception> exceptionType) {
        List<IRequestExceptionHandler<?, ?, ?>> result =
                new ArrayList<>();
        for (GlobalEntry entry : globalHandlers) {
            if (entry.requestType() != null
                    && entry.requestType()
                    .isAssignableFrom(requestType)
                    && entry.exceptionType()
                    .isAssignableFrom(exceptionType)) {
                result.add(entry.handler());
            }
        }
        List<HandlerEntry> specific = handlers.get(requestType);
        if (specific != null) {
            for (HandlerEntry entry : specific) {
                if (entry.exceptionType()
                        .isAssignableFrom(exceptionType)) {
                    result.add(entry.handler());
                }
            }
        }
        result.sort(Comparator.comparingInt(
                this::getHandlerOrder));
        return Collections.unmodifiableList(result);
    }

    public void freeze() {
        this.frozen = true;
        int total = globalHandlers.size()
                + handlers.values().stream()
                .mapToInt(List::size).sum();
        logger.info(
                "ExceptionHandlerRegistry frozen with {} handlers",
                total);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int getCount() {
        return globalHandlers.size()
                + handlers.values().stream()
                .mapToInt(List::size).sum();
    }

    /**
     * Resolves the execution order for an exception handler based on
     * {@link Order} annotation or {@link Ordered} interface.
     */
    private int getHandlerOrder(
            IRequestExceptionHandler<?, ?, ?> handler) {
        Class<?> clazz = handler.getClass();
        Order orderAnnotation = clazz.getAnnotation(Order.class);
        if (orderAnnotation != null) {
            return orderAnnotation.value();
        }
        if (handler instanceof Ordered ordered) {
            return ordered.getOrder();
        }
        return Ordered.LOWEST_PRECEDENCE;
    }
}
