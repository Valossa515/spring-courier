package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Dispatches requests through the CQRS pipeline, invoking synchronous or
 * asynchronous handlers as needed. Also supports publishing notifications
 * to multiple handlers.
 */
public class Courier {
    private static final Logger logger = LoggerFactory.getLogger(Courier.class);
    private static final long DEFAULT_ASYNC_TIMEOUT_MS = 30_000;
    private static final long MIN_ASYNC_TIMEOUT_MS = 100;
    private static final long MAX_ASYNC_TIMEOUT_MS = 600_000;

    private static final Set<String> ALLOWED_HANDLER_METHOD_NAMES = Set.of("handle", "execute");

    /**
     * Cache for request handler methods (accepts "handle" or "execute").
     */
    private static final ConcurrentHashMap<Class<?>, Method> REQUEST_METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * Cache for notification handler methods (accepts only "handle").
     */
    private static final ConcurrentHashMap<Class<?>, Method> NOTIFICATION_METHOD_CACHE = new ConcurrentHashMap<>();

    private final HandlerRegistry handlerRegistry;
    private final NotificationRegistry notificationRegistry;
    private final PipelineExecutor pipelineExecutor;
    private final Executor asyncExecutor;
    private final long asyncTimeoutMs;

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor) {
        this(handlerRegistry, notificationRegistry, pipelineExecutor, null, DEFAULT_ASYNC_TIMEOUT_MS);
    }

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor,
                   Executor asyncExecutor) {
        this(handlerRegistry, notificationRegistry, pipelineExecutor, asyncExecutor, DEFAULT_ASYNC_TIMEOUT_MS);
    }

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor,
                   Executor asyncExecutor,
                   long asyncTimeoutMs) {
        this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry must not be null");
        this.notificationRegistry = Objects.requireNonNull(notificationRegistry, "notificationRegistry must not be null");
        this.pipelineExecutor = Objects.requireNonNull(pipelineExecutor, "pipelineExecutor must not be null");
        this.asyncExecutor = asyncExecutor;
        this.asyncTimeoutMs = clampTimeout(asyncTimeoutMs);
        logger.info("Courier initialized with {} registered handlers (asyncTimeoutMs={})",
                handlerRegistry.getHandlerCount(), this.asyncTimeoutMs);
    }

    /**
     * Sends a request and returns a typed {@link Response}.
     */
    public <R> Response<R> send(@NotNull IRequest<R> request) {
        logger.debug("Sending request: {}", request.getClass().getSimpleName());

        Object handler = handlerRegistry.getHandler(request.getClass());

        Response<R> result = pipelineExecutor.execute(request, () -> invokeHandler(handler, request));

        if (result == null) {
            return Response.success(null);
        }

        return result;
    }

    /**
     * Invokes the handler, transparently supporting "handle" and "execute"
     * method conventions.
     */
    @SuppressWarnings("unchecked")
    private <R> Response<R> invokeHandler(Object handler, IRequest<R> request) {
        try {
            validateHandler(handler);
            Method method = getCachedMethod(handler.getClass());
            long start = System.nanoTime();

            Object result = method.invoke(handler, request);

            if (result instanceof CompletableFuture<?> future) {
                try {
                    result = future.get(asyncTimeoutMs, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    logger.error("Handler timed out after {}ms: {}", asyncTimeoutMs,
                            handler.getClass().getSimpleName());
                    return Response.error("Handler timed out", 504);
                }
            }

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            logger.debug("Handler executed in {}ms: {} -> {}", durationMs,
                    request.getClass().getSimpleName(), handler.getClass().getSimpleName());

            if (result == null) {
                return Response.success(null);
            }
            if (result instanceof Response<?> response) {
                return (Response<R>) response;
            }

            return Response.success((R) result);

        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            logger.error("Handler execution failed for request type: {}",
                    request.getClass().getSimpleName(), target);
            return Response.error(sanitizeErrorMessage(target.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Courier runtime error for request type: {}",
                    request.getClass().getSimpleName(), e);
            return Response.error(sanitizeErrorMessage(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Handler interrupted for request type: {}",
                    request.getClass().getSimpleName(), e);
            return Response.error("Handler execution was interrupted");
        } catch (Exception e) {
            logger.error("Failed to invoke handler for request type: {}",
                    request.getClass().getSimpleName(), e);
            return Response.error("An internal error occurred");
        }
    }

    /**
     * Returns a cached Method for the given handler class, resolving and caching
     * on first access. Only public methods from recognized handler interfaces are
     * resolved — private/protected methods are never made accessible.
     */
    private @NotNull Method getCachedMethod(@NotNull Class<?> handlerClass) {
        return REQUEST_METHOD_CACHE.computeIfAbsent(handlerClass, clazz -> {
            for (Method method : clazz.getMethods()) {
                if (ALLOWED_HANDLER_METHOD_NAMES.contains(method.getName())
                        && method.getParameterCount() == 1) {
                    return method;
                }
            }
            throw new HandlerMethodNotFoundException(clazz.getSimpleName(), "handle/execute");
        });
    }

    /**
     * Publishes a notification to all registered handlers.
     *
     * <p><strong>Execution model:</strong> handlers are invoked <em>sequentially</em>
     * in the order they were registered. If a handler throws an exception,
     * it will be logged at {@code ERROR} level but <strong>not propagated</strong>,
     * so subsequent handlers are still executed.
     *
     * <p>If no handler is registered for the given notification type, a {@code WARN}
     * message is logged and the method returns immediately.
     *
     * @param notification the notification to publish
     * @see #publishAsync(INotification) for non-blocking variant
     */
    public void publish(@NotNull INotification notification) {
        logger.debug("Publishing notification: {}", notification.getClass().getSimpleName());

        List<Object> handlers = notificationRegistry.getHandlers(notification.getClass());

        if (handlers.isEmpty()) {
            logger.warn("No handler registered for notification: {}", notification.getClass().getSimpleName());
            return;
        }

        for (Object handler : handlers) {
            try {
                validateNotificationHandler(handler);
                Method method = getCachedHandleMethod(handler.getClass());
                method.invoke(handler, notification);
                logger.debug("Notification handler executed: {} -> {}",
                        notification.getClass().getSimpleName(), handler.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Error executing notification handler for notification type: {}",
                        notification.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Publishes a notification asynchronously to all registered handlers.
     *
     * @param notification the notification to publish
     * @return CompletableFuture that completes when all handlers finish
     */
    public CompletableFuture<Void> publishAsync(@NotNull INotification notification) {
        if (asyncExecutor != null) {
            return CompletableFuture.runAsync(() -> publish(notification), asyncExecutor);
        }
        return CompletableFuture.runAsync(() -> publish(notification));
    }

    private @NotNull Method getCachedHandleMethod(@NotNull Class<?> handlerClass) {
        return NOTIFICATION_METHOD_CACHE.computeIfAbsent(handlerClass, clazz -> {
            for (Method method : clazz.getMethods()) {
                if ("handle".equals(method.getName()) && method.getParameterCount() == 1) {
                    return method;
                }
            }
            throw new HandlerMethodNotFoundException(clazz.getSimpleName(), "handle");
        });
    }

    /**
     * Validates that the handler is a recognized handler type (CommandHandler or QueryHandler).
     */
    private void validateHandler(Object handler) {
        if (!(handler instanceof CommandHandler<?, ?>) && !(handler instanceof QueryHandler<?, ?>)) {
            throw new IllegalArgumentException(
                    "Handler must implement CommandHandler or QueryHandler: " + handler.getClass().getSimpleName());
        }
    }

    /**
     * Validates that the handler is a recognized notification handler type.
     */
    private void validateNotificationHandler(Object handler) {
        if (!(handler instanceof NotificationHandler<?>)) {
            throw new IllegalArgumentException(
                    "Handler must implement NotificationHandler: " + handler.getClass().getSimpleName());
        }
    }

    /**
     * Clamps the timeout value to a safe range between {@link #MIN_ASYNC_TIMEOUT_MS}
     * and {@link #MAX_ASYNC_TIMEOUT_MS}.
     */
    private static long clampTimeout(long timeoutMs) {
        if (timeoutMs <= 0) {
            return DEFAULT_ASYNC_TIMEOUT_MS;
        }
        if (timeoutMs < MIN_ASYNC_TIMEOUT_MS) {
            logger.warn("Async timeout {}ms is below minimum, using {}ms", timeoutMs, MIN_ASYNC_TIMEOUT_MS);
            return MIN_ASYNC_TIMEOUT_MS;
        }
        if (timeoutMs > MAX_ASYNC_TIMEOUT_MS) {
            logger.warn("Async timeout {}ms exceeds maximum, using {}ms", timeoutMs, MAX_ASYNC_TIMEOUT_MS);
            return MAX_ASYNC_TIMEOUT_MS;
        }
        return timeoutMs;
    }

    /**
     * Sanitizes error messages to prevent information disclosure. Strips
     * class names, file paths, and stack trace details from messages exposed
     * to callers.
     */
    private static String sanitizeErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "An internal error occurred";
        }
        // Remove potential class/package names and file paths
        String sanitized = message.replaceAll("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*){2,}", "***");
        // Remove potential file paths
        sanitized = sanitized.replaceAll("(/[\\w.-]+){2,}", "***");
        // Truncate overly long messages
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...";
        }
        return sanitized;
    }

    /**
     * Exception for missing handler methods.
     */
    public static class HandlerMethodNotFoundException extends RuntimeException {
        public HandlerMethodNotFoundException(String handlerClassName, String expectedMethod) {
            super("No handle method (" + expectedMethod + ") found in handler: " + handlerClassName);
        }
    }
}