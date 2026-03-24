package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.config.CourierProperties;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import io.github.valossa515.spring_courier.core.interfaces.IRequestPostProcessor;
import io.github.valossa515.spring_courier.core.interfaces.IRequestPreProcessor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.support.ExceptionHandlerRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.PostProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.PreProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Dispatches requests through the CQRS pipeline, invoking synchronous or
 * asynchronous handlers as needed. Also supports publishing notifications
 * to multiple handlers with configurable strategies.
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
     */
    private static final Executor VIRTUAL_THREAD_EXECUTOR =
            runnable -> Thread.ofVirtual().start(runnable);

    private final HandlerRegistry handlerRegistry;
    private final NotificationRegistry notificationRegistry;
    private final PipelineExecutor pipelineExecutor;
    private final Executor asyncExecutor;
    private final long asyncTimeoutMs;
    private final PreProcessorRegistry preProcessorRegistry;
    private final PostProcessorRegistry postProcessorRegistry;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final CourierProperties.PublishStrategy publishStrategy;

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor) {
        this(handlerRegistry, notificationRegistry, pipelineExecutor,
                null, DEFAULT_ASYNC_TIMEOUT_MS, null, null, null,
                CourierProperties.PublishStrategy.SEQUENTIAL);
    }

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor,
                   Executor asyncExecutor) {
        this(handlerRegistry, notificationRegistry, pipelineExecutor,
                asyncExecutor, DEFAULT_ASYNC_TIMEOUT_MS, null, null, null,
                CourierProperties.PublishStrategy.SEQUENTIAL);
    }

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor,
                   Executor asyncExecutor,
                   long asyncTimeoutMs) {
        this(handlerRegistry, notificationRegistry, pipelineExecutor,
                asyncExecutor, asyncTimeoutMs, null, null, null,
                CourierProperties.PublishStrategy.SEQUENTIAL);
    }

    @SuppressWarnings("java:S107")
    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor,
                   Executor asyncExecutor,
                   long asyncTimeoutMs,
                   PreProcessorRegistry preProcessorRegistry,
                   PostProcessorRegistry postProcessorRegistry,
                   ExceptionHandlerRegistry exceptionHandlerRegistry,
                   CourierProperties.PublishStrategy publishStrategy) {
        this.handlerRegistry = Objects.requireNonNull(
                handlerRegistry, "handlerRegistry must not be null");
        this.notificationRegistry = Objects.requireNonNull(
                notificationRegistry, "notificationRegistry must not be null");
        this.pipelineExecutor = Objects.requireNonNull(
                pipelineExecutor, "pipelineExecutor must not be null");
        this.asyncExecutor = asyncExecutor;
        this.asyncTimeoutMs = asyncTimeoutMs > 0
                ? asyncTimeoutMs : DEFAULT_ASYNC_TIMEOUT_MS;
        this.preProcessorRegistry = preProcessorRegistry;
        this.postProcessorRegistry = postProcessorRegistry;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.publishStrategy = publishStrategy != null
                ? publishStrategy
                : CourierProperties.PublishStrategy.SEQUENTIAL;
        logger.info("Courier initialized with {} registered handlers "
                        + "(asyncTimeoutMs={}, publishStrategy={})",
                handlerRegistry.getHandlerCount(), this.asyncTimeoutMs,
                this.publishStrategy);
    }

    /**
     * Sends a request and returns a typed {@link Response}.
     *
     * @param request the request to dispatch; must not be {@code null}
     * @throws NullPointerException if {@code request} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public <R> Response<R> send(@NotNull IRequest<R> request) {
        Objects.requireNonNull(request, "request must not be null");
        logger.debug("Sending request: {}", request.getClass().getSimpleName());

        runPreProcessors(request);

        Object handler = handlerRegistry.getHandler(request.getClass());

        Response<R> result;
        try {
            result = pipelineExecutor.execute(
                    request, () -> invokeHandlerRaw(handler, request));
        } catch (RuntimeException ex) {
            Response<R> handled = tryExceptionHandlers(request, ex);
            if (handled != null) {
                return handled;
            }
            throw ex;
        }

        if (result == null) {
            result = Response.success(null);
        }

        if (!result.isSuccess() && exceptionHandlerRegistry != null) {
            Response<R> handled = tryExceptionHandlersForResponse(
                    request, result);
            if (handled != null) {
                result = handled;
            }
        }

        runPostProcessors(request, result);
        return result;
    }

    /**
     * Sends a request asynchronously and returns a
     * {@link CompletableFuture} wrapping the typed {@link Response}.
     *
     * @param request the request to dispatch; must not be {@code null}
     * @return a future that completes with the response
     * @throws NullPointerException if {@code request} is {@code null}
     */
    public <R> CompletableFuture<Response<R>> sendAsync(
            @NotNull IRequest<R> request) {
        Objects.requireNonNull(request, "request must not be null");
        Executor executor = asyncExecutor != null
                ? asyncExecutor : VIRTUAL_THREAD_EXECUTOR;
        return CompletableFuture.supplyAsync(() -> send(request), executor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <R> void runPreProcessors(IRequest<R> request) {
        if (preProcessorRegistry == null) {
            return;
        }
        List<IRequestPreProcessor<?>> processors =
                preProcessorRegistry.getProcessors(request.getClass());
        for (IRequestPreProcessor processor : processors) {
            try {
                processor.process(request);
            } catch (Exception e) {
                logger.error("Pre-processor {} failed for {}: {}",
                        processor.getClass().getSimpleName(),
                        request.getClass().getSimpleName(),
                        e.getMessage(), e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <R> void runPostProcessors(IRequest<R> request,
                                       Response<R> response) {
        if (postProcessorRegistry == null) {
            return;
        }
        List<IRequestPostProcessor<?, ?>> processors =
                postProcessorRegistry.getProcessors(request.getClass());
        for (IRequestPostProcessor processor : processors) {
            try {
                processor.process(request, response);
            } catch (Exception e) {
                logger.error("Post-processor {} failed for {}: {}",
                        processor.getClass().getSimpleName(),
                        request.getClass().getSimpleName(),
                        e.getMessage(), e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <R> Response<R> tryExceptionHandlers(
            IRequest<?> request, RuntimeException ex) {
        if (exceptionHandlerRegistry == null) {
            return null;
        }
        List<IRequestExceptionHandler<?, ?, ?>> handlers =
                exceptionHandlerRegistry.getHandlers(
                        request.getClass(), ex.getClass()
                                .asSubclass(Exception.class));
        for (IRequestExceptionHandler handler : handlers) {
            try {
                Response result = handler.handle(request, ex);
                if (result != null) {
                    return (Response<R>) result;
                }
            } catch (Exception e) {
                logger.error("Exception handler {} failed: {}",
                        handler.getClass().getSimpleName(),
                        e.getMessage(), e);
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <R> Response<R> tryExceptionHandlersForResponse(
            IRequest<?> request, Response<R> errorResponse) {
        if (exceptionHandlerRegistry == null) {
            return null;
        }
        String exTypeName = errorResponse.getExceptionType();
        List<IRequestExceptionHandler<?, ?, ?>> handlers =
                exceptionHandlerRegistry.getHandlers(
                        request.getClass());
        if (handlers.isEmpty()) {
            return null;
        }
        RuntimeException syntheticEx = new RuntimeException(
                errorResponse.getError());
        for (IRequestExceptionHandler handler : handlers) {
            try {
                Response result = handler.handle(
                        request, syntheticEx);
                if (result != null) {
                    return (Response<R>) result;
                }
            } catch (Exception e) {
                logger.error("Exception handler {} failed: {}",
                        handler.getClass().getSimpleName(),
                        e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Invokes the handler and returns the raw result object without
     * wrapping in {@link Response}. This is the version used by
     * {@link PipelineExecutor}, whose {@code normalize()} step handles
     * the {@link Response} wrapping. Timeout and async-handler
     * unwrapping are still performed here.
     *
     * <p>For error conditions that cannot be represented as raw
     * results (timeouts, execution errors), the method returns a
     * {@link Response#error} directly so that {@code normalize()}
     * passes it through unchanged.
     */
    private <R> Object invokeHandlerRaw(Object handler,
                                        IRequest<R> request) {
        try {
            Method method = getCachedMethod(handler.getClass());
            long start = System.nanoTime();

            CompletableFuture<Object> invocationFuture =
                    CompletableFuture.supplyAsync(
                            () -> reflectiveInvoke(
                                    method, handler, request),
                            VIRTUAL_THREAD_EXECUTOR);

            Object result;
            try {
                result = invocationFuture.get(
                        asyncTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                invocationFuture.cancel(true);
                logger.error("Handler timed out after {}ms: {}",
                        asyncTimeoutMs,
                        handler.getClass().getSimpleName());
                return Response.error(
                        "Handler timed out after "
                                + asyncTimeoutMs + "ms",
                        504,
                        TimeoutException.class.getSimpleName());
            } catch (ExecutionException e) {
                Throwable cause = unwrapExecutionCause(e);
                logger.error("Handler error: {}",
                        cause.getMessage(), cause);
                return Response.error(cause);
            }

            if (result instanceof CompletableFuture<?> future) {
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - start);
                long remainingMs = Math.max(
                        asyncTimeoutMs - elapsedMs, 1);
                try {
                    result = future.get(
                            remainingMs, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    logger.error(
                            "Handler timed out after {}ms: {}",
                            asyncTimeoutMs,
                            handler.getClass().getSimpleName());
                    return Response.error(
                            "Handler timed out after "
                                    + asyncTimeoutMs + "ms",
                            504,
                            TimeoutException.class
                                    .getSimpleName());
                } catch (ExecutionException e) {
                    Throwable cause = unwrapExecutionCause(e);
                    logger.error("Async handler error: {}",
                            cause.getMessage(), cause);
                    return Response.error(cause);
                }
            }

            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - start);
            logger.debug("Handler executed in {}ms: {} -> {}",
                    durationMs,
                    request.getClass().getSimpleName(),
                    handler.getClass().getSimpleName());

            return result;

        } catch (RuntimeException e) {
            logger.error("Courier runtime error: {}",
                    e.getMessage(), e);
            return Response.error(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Handler interrupted: {}",
                    e.getMessage(), e);
            return Response.error(e);
        }
    }

    /**
     * Invokes the handler and wraps the result in a {@link Response}.
     * Used for direct handler invocation outside the pipeline.
     */
    @SuppressWarnings("unchecked")
    private <R> Response<R> invokeHandler(Object handler,
                                          IRequest<R> request) {
        Object raw = invokeHandlerRaw(handler, request);
        if (raw == null) {
            return Response.success(null);
        }
        if (raw instanceof Response<?> response) {
            return (Response<R>) response;
        }
        return Response.success((R) raw);
    }

    /**
     * Reflective method invocation helper used inside
     * {@link CompletableFuture#supplyAsync}.
     */
    private static Object reflectiveInvoke(Method method,
                                           Object handler, Object request) {
        try {
            return method.invoke(handler, request);
        } catch (InvocationTargetException e) {
            throw new CompletionException(
                    e.getTargetException() != null
                            ? e.getTargetException() : e);
        } catch (ReflectiveOperationException e) {
            throw new CompletionException(e);
        }
    }

    /**
     * Unwraps the real cause from an {@link ExecutionException}.
     */
    private static Throwable unwrapExecutionCause(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof CompletionException ce) {
            return ce.getCause() != null ? ce.getCause() : ce;
        }
        return cause != null ? cause : e;
    }

    /**
     * Returns a cached Method for the given handler class.
     */
    @SuppressWarnings("java:S3011")
    private @NotNull Method getCachedMethod(
            @NotNull Class<?> handlerClass) {
        Method cached = REQUEST_METHOD_CACHE.get(handlerClass);
        if (cached != null) {
            return cached;
        }
        for (Method method : handlerClass.getMethods()) {
            if ((method.getName().equals("handle")
                    || method.getName().equals("execute"))
                    && method.getParameterCount() == 1
                    && IRequest.class.isAssignableFrom(
                    method.getParameterTypes()[0])) {
                method.setAccessible(true);
                REQUEST_METHOD_CACHE.put(handlerClass, method);
                return method;
            }
        }
        throw new HandlerMethodNotFoundException(
                handlerClass.getName(), "handle/execute");
    }

    /**
     * Publishes a notification to all registered handlers using the
     * configured {@link CourierProperties.PublishStrategy}.
     *
     * @param notification the notification to publish
     * @throws NullPointerException if {@code notification} is {@code null}
     */
    public void publish(@NotNull INotification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        logger.debug("Publishing notification: {}",
                notification.getClass().getSimpleName());

        List<Object> handlers = notificationRegistry.getHandlers(
                notification.getClass());

        if (handlers.isEmpty()) {
            logger.warn("No handler registered for notification: {}",
                    notification.getClass().getSimpleName());
            return;
        }

        switch (publishStrategy) {
            case PARALLEL_WHEN_ALL ->
                    publishParallel(notification, handlers);
            case STOP_ON_FIRST_ERROR ->
                    publishStopOnFirstError(notification, handlers);
            default ->
                    publishSequential(notification, handlers);
        }
    }

    private void publishSequential(INotification notification,
                                   List<Object> handlers) {
        for (Object handler : handlers) {
            invokeNotificationHandler(notification, handler);
        }
    }

    private void publishParallel(INotification notification,
                                 List<Object> handlers) {
        Executor executor = asyncExecutor != null
                ? asyncExecutor : VIRTUAL_THREAD_EXECUTOR;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Object handler : handlers) {
            futures.add(CompletableFuture.runAsync(
                    () -> invokeNotificationHandler(
                            notification, handler), executor));
        }
        CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])).join();
    }

    private void publishStopOnFirstError(INotification notification,
                                         List<Object> handlers) {
        for (Object handler : handlers) {
            try {
                Method method = getCachedHandleMethod(handler.getClass());
                method.invoke(handler, notification);
                logger.debug("Notification handler executed: {} -> {}",
                        notification.getClass().getSimpleName(),
                        handler.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error(
                        "Notification handler {} failed, stopping: {}",
                        handler.getClass().getSimpleName(),
                        e.getMessage(), e);
                return;
            }
        }
    }

    private void invokeNotificationHandler(INotification notification,
                                           Object handler) {
        try {
            Method method = getCachedHandleMethod(handler.getClass());
            method.invoke(handler, notification);
            logger.debug("Notification handler executed: {} -> {}",
                    notification.getClass().getSimpleName(),
                    handler.getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Error executing notification handler {}: {}",
                    handler.getClass().getSimpleName(),
                    e.getMessage(), e);
        }
    }

    /**
     * Publishes a notification asynchronously to all registered handlers.
     *
     * @param notification the notification to publish
     * @return CompletableFuture that completes when all handlers finish
     * @throws NullPointerException if {@code notification} is {@code null}
     */
    public CompletableFuture<Void> publishAsync(
            @NotNull INotification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        if (asyncExecutor != null) {
            return CompletableFuture.runAsync(
                    () -> publish(notification), asyncExecutor);
        }
        return CompletableFuture.runAsync(
                () -> publish(notification), VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Returns a cached Method for notification handlers.
     */
    @SuppressWarnings("java:S3011")
    private @NotNull Method getCachedHandleMethod(
            @NotNull Class<?> handlerClass) {
        Method cached = NOTIFICATION_METHOD_CACHE.get(handlerClass);
        if (cached != null) {
            return cached;
        }
        for (Method method : handlerClass.getMethods()) {
            if (method.getName().equals("handle")
                    && method.getParameterCount() == 1
                    && INotification.class.isAssignableFrom(
                    method.getParameterTypes()[0])) {
                method.setAccessible(true);
                NOTIFICATION_METHOD_CACHE.put(handlerClass, method);
                return method;
            }
        }
        throw new HandlerMethodNotFoundException(
                handlerClass.getName(), "handle");
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
    public static class HandlerMethodNotFoundException
            extends RuntimeException {
        public HandlerMethodNotFoundException(String handlerClassName,
                                              String expectedMethod) {
            super("No handle method (" + expectedMethod
                    + ") found in handler: " + handlerClassName);
        }
    }
}
