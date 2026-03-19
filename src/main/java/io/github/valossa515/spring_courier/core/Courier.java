package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Dispatches requests through the CQRS pipeline, invoking synchronous or
 * asynchronous handlers as needed. Also supports publishing notifications
 * to multiple handlers.
 */
public class Courier {
    private static final Logger logger = LoggerFactory.getLogger(Courier.class);
    private static final long DEFAULT_ASYNC_TIMEOUT_MS = 30_000;
    private static final int MAX_CACHE_SIZE = 1024;

    /**
     * Bounded LRU cache for request handler methods (accepts "handle" or "execute").
     * Limited to {@value #MAX_CACHE_SIZE} entries to prevent memory exhaustion
     * in environments with class reloading.
     */
    private static final Map<Class<?>, Method> REQUEST_METHOD_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Class<?>, Method> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            });

    /**
     * Bounded LRU cache for notification handler methods (accepts only "handle").
     * Limited to {@value #MAX_CACHE_SIZE} entries to prevent memory exhaustion.
     */
    private static final Map<Class<?>, Method> NOTIFICATION_METHOD_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Class<?>, Method> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            });

    /**
     * Default executor for {@link #publishAsync} when no custom executor is
     * provided. Starts a new virtual thread per task for lightweight, scalable
     * asynchronous execution without blocking platform threads.
     *
     * <p>Unlike {@code Executors.newVirtualThreadPerTaskExecutor()}, this
     * implementation does not hold any resources that require shutdown,
     * making it safe to store as a static field and compatible with
     * classloader unloading.
     */
    private static final Executor VIRTUAL_THREAD_EXECUTOR =
            runnable -> Thread.ofVirtual().start(runnable);

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
        this.asyncTimeoutMs = asyncTimeoutMs > 0 ? asyncTimeoutMs : DEFAULT_ASYNC_TIMEOUT_MS;
        logger.info("Courier initialized with {} registered handlers (asyncTimeoutMs={})",
                handlerRegistry.getHandlerCount(), this.asyncTimeoutMs);
    }

    /**
     * Sends a request and returns a typed {@link Response}.
     *
     * @param request the request to dispatch; must not be {@code null}
     * @throws NullPointerException if {@code request} is {@code null}
     */
    public <R> Response<R> send(@NotNull IRequest<R> request) {
        Objects.requireNonNull(request, "request must not be null");
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
            Method method = getCachedMethod(handler.getClass());
            long start = System.nanoTime();

            Object result = method.invoke(handler, request);

            if (result instanceof CompletableFuture<?> future) {
                try {
                    result = future.get(asyncTimeoutMs, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    logger.error("Handler timed out after {}ms: {}", asyncTimeoutMs,
                            handler.getClass().getSimpleName());
                    return Response.error("Handler timed out after " + asyncTimeoutMs + "ms", 504);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    logger.error("Async handler error: {}", cause.getMessage(), cause);
                    return Response.error(cause);
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
            logger.error("Handler error: {}", target.getMessage(), target);
            return Response.error(target);
        } catch (RuntimeException e) {
            logger.error("Courier runtime error: {}", e.getMessage(), e);
            return Response.error(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Handler interrupted: {}", e.getMessage(), e);
            return Response.error(e);
        } catch (Exception e) {
            logger.error("Failed to invoke handler: {}", e.getMessage(), e);
            return Response.error(e);
        }
    }

    /**
     * Returns a cached Method for the given handler class, resolving and caching
     * on first access. The method is made accessible once during caching, which
     * avoids repeated setAccessible calls on concurrent invocations.
     *
     * <p>Only methods whose single parameter is assignable from {@link IRequest}
     * are matched, preventing unrelated methods from being invoked via reflection.
     */
    @SuppressWarnings("java:S3011")
    private @NotNull Method getCachedMethod(@NotNull Class<?> handlerClass) {
        Method cached = REQUEST_METHOD_CACHE.get(handlerClass);
        if (cached != null) {
            return cached;
        }
        for (Method method : handlerClass.getMethods()) {
            if ((method.getName().equals("handle") || method.getName().equals("execute"))
                    && method.getParameterCount() == 1
                    && IRequest.class.isAssignableFrom(method.getParameterTypes()[0])) {
                method.setAccessible(true);
                REQUEST_METHOD_CACHE.put(handlerClass, method);
                return method;
            }
        }
        throw new HandlerMethodNotFoundException(handlerClass.getName(), "handle/execute");
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
     * @param notification the notification to publish; must not be {@code null}
     * @throws NullPointerException if {@code notification} is {@code null}
     * @see #publishAsync(INotification) for non-blocking variant
     */
    public void publish(@NotNull INotification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        logger.debug("Publishing notification: {}", notification.getClass().getSimpleName());

        List<Object> handlers = notificationRegistry.getHandlers(notification.getClass());

        if (handlers.isEmpty()) {
            logger.warn("No handler registered for notification: {}", notification.getClass().getSimpleName());
            return;
        }

        for (Object handler : handlers) {
            try {
                Method method = getCachedHandleMethod(handler.getClass());
                method.invoke(handler, notification);
                logger.debug("Notification handler executed: {} -> {}",
                        notification.getClass().getSimpleName(), handler.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Error executing notification handler {}: {}",
                        handler.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Publishes a notification asynchronously to all registered handlers.
     *
     * <p>If a dedicated async executor was configured via the constructor, it
     * is used to run the notification dispatch. Otherwise, a new
     * <strong>virtual thread</strong> (Java 21) is started per invocation,
     * providing lightweight concurrency without blocking platform threads.
     *
     * @param notification the notification to publish; must not be {@code null}
     * @return CompletableFuture that completes when all handlers finish
     * @throws NullPointerException if {@code notification} is {@code null}
     */
    public CompletableFuture<Void> publishAsync(@NotNull INotification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        if (asyncExecutor != null) {
            return CompletableFuture.runAsync(() -> publish(notification), asyncExecutor);
        }
        return CompletableFuture.runAsync(() -> publish(notification), VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Returns a cached Method for notification handlers. Only methods whose
     * single parameter is assignable from {@link INotification} are matched.
     */
    @SuppressWarnings("java:S3011")
    private @NotNull Method getCachedHandleMethod(@NotNull Class<?> handlerClass) {
        Method cached = NOTIFICATION_METHOD_CACHE.get(handlerClass);
        if (cached != null) {
            return cached;
        }
        for (Method method : handlerClass.getMethods()) {
            if (method.getName().equals("handle")
                    && method.getParameterCount() == 1
                    && INotification.class.isAssignableFrom(method.getParameterTypes()[0])) {
                method.setAccessible(true);
                NOTIFICATION_METHOD_CACHE.put(handlerClass, method);
                return method;
            }
        }
        throw new HandlerMethodNotFoundException(handlerClass.getName(), "handle");
    }

    /**
     * Clears the internal method caches. Intended for use by tests and
     * class-reloading scenarios.
     */
    static void clearMethodCaches() {
        REQUEST_METHOD_CACHE.clear();
        NOTIFICATION_METHOD_CACHE.clear();
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
