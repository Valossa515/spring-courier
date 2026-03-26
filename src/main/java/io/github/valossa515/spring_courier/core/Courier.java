package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.annotations.Timeout;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PostProcessor;
import io.github.valossa515.spring_courier.core.pipelines.PreProcessor;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.CourierContext;
import io.github.valossa515.spring_courier.core.support.CourierContextHolder;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationHandlerException;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
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
 * to multiple handlers with configurable publishing strategies.
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
     * Default executor for async operations when no custom executor is
     * provided. Starts a new virtual thread per task for lightweight, scalable
     * asynchronous execution without blocking platform threads.
     */
    private static final Executor VIRTUAL_THREAD_EXECUTOR =
            runnable -> Thread.ofVirtual().start(runnable);

    private final HandlerRegistry handlerRegistry;
    private final NotificationRegistry notificationRegistry;
    private final PipelineExecutor pipelineExecutor;
    private final ProcessorRegistry processorRegistry;
    private final Executor asyncExecutor;
    private final long asyncTimeoutMs;
    private final NotificationPublishingStrategy notificationStrategy;

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor) {
        this(handlerRegistry, notificationRegistry, pipelineExecutor,
                null, null, DEFAULT_ASYNC_TIMEOUT_MS,
                NotificationPublishingStrategy.SEQUENTIAL);
    }

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor,
                   Executor asyncExecutor) {
        this(handlerRegistry, notificationRegistry, pipelineExecutor,
                null, asyncExecutor, DEFAULT_ASYNC_TIMEOUT_MS,
                NotificationPublishingStrategy.SEQUENTIAL);
    }

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor,
                   Executor asyncExecutor,
                   long asyncTimeoutMs) {
        this(handlerRegistry, notificationRegistry, pipelineExecutor,
                null, asyncExecutor, asyncTimeoutMs,
                NotificationPublishingStrategy.SEQUENTIAL);
    }

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor,
                   ProcessorRegistry processorRegistry,
                   Executor asyncExecutor,
                   long asyncTimeoutMs,
                   @NotNull NotificationPublishingStrategy notificationStrategy) {
        this.handlerRegistry = Objects.requireNonNull(
                handlerRegistry, "handlerRegistry must not be null");
        this.notificationRegistry = Objects.requireNonNull(
                notificationRegistry, "notificationRegistry must not be null");
        this.pipelineExecutor = Objects.requireNonNull(
                pipelineExecutor, "pipelineExecutor must not be null");
        this.processorRegistry = processorRegistry;
        this.asyncExecutor = asyncExecutor;
        this.asyncTimeoutMs = asyncTimeoutMs > 0
                ? asyncTimeoutMs : DEFAULT_ASYNC_TIMEOUT_MS;
        this.notificationStrategy = Objects.requireNonNull(
                notificationStrategy, "notificationStrategy must not be null");
        logger.info("Courier initialized with {} registered handlers "
                        + "(asyncTimeoutMs={}, notificationStrategy={})",
                handlerRegistry.getHandlerCount(), this.asyncTimeoutMs,
                this.notificationStrategy);
    }

    /**
     * Sends a request synchronously and returns a typed {@link Response}.
     *
     * @param request the request to dispatch; must not be {@code null}
     * @throws NullPointerException if {@code request} is {@code null}
     */
    public <R> Response<R> send(@NotNull IRequest<R> request) {
        Objects.requireNonNull(request, "request must not be null");
        logger.debug("Sending request: {}", request.getClass().getSimpleName());

        boolean contextOwner = false;
        if (CourierContextHolder.peekContext() == null) {
            CourierContextHolder.setContext(CourierContext.create());
            contextOwner = true;
        }

        try {
            return doSend(request);
        } finally {
            if (contextOwner) {
                CourierContextHolder.clear();
            }
        }
    }

    private <R> Response<R> doSend(IRequest<R> request) {
        runPreProcessors(request);

        Object handler = handlerRegistry.getHandler(request.getClass());

        Response<R> result;
        try {
            result = pipelineExecutor.execute(
                    request, () -> invokeHandler(handler, request));
        } catch (Exception ex) {
            Response<R> handled = tryExceptionHandlers(request, ex);
            if (handled != null) {
                return handled;
            }
            throw ex;
        }

        if (result == null) {
            result = Response.success(null);
        }

        runPostProcessors(request, result);
        return result;
    }

    /**
     * Sends a request asynchronously, returning a
     * {@link CompletableFuture} that completes with the response.
     *
     * <p>The request is dispatched on a virtual thread (or the
     * configured async executor), keeping the calling thread free.
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
        CourierContext ctx = CourierContextHolder.peekContext();
        return CompletableFuture.supplyAsync(() -> {
            if (ctx != null) {
                CourierContextHolder.setContext(ctx);
            }
            try {
                return send(request);
            } finally {
                CourierContextHolder.clear();
            }
        }, executor);
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
            long effectiveTimeout = resolveTimeout(request);

            CourierContext ctx = CourierContextHolder.peekContext();
            CompletableFuture<Object> invocationFuture =
                    CompletableFuture.supplyAsync(() -> {
                        if (ctx != null) {
                            CourierContextHolder.setContext(ctx);
                        }
                        try {
                            return reflectiveInvoke(
                                    method, handler, request);
                        } finally {
                            CourierContextHolder.clear();
                        }
                    }, VIRTUAL_THREAD_EXECUTOR);

            Object result;
            try {
                result = invocationFuture.get(
                        effectiveTimeout, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                invocationFuture.cancel(true);
                logger.error("Handler timed out after {}ms: {}",
                        effectiveTimeout, handler.getClass().getSimpleName());
                return Response.error(
                        "Handler timed out after " + effectiveTimeout + "ms",
                        504, TimeoutException.class.getSimpleName());
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
                        effectiveTimeout - elapsedMs, 1);
                try {
                    result = future.get(
                            remainingMs, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    logger.error("Handler timed out after {}ms: {}",
                            effectiveTimeout,
                            handler.getClass().getSimpleName());
                    return Response.error(
                            "Handler timed out after "
                                    + effectiveTimeout + "ms",
                            504,
                            TimeoutException.class.getSimpleName());
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

            if (result == null) {
                return Response.success(null);
            }
            if (result instanceof Response<?> response) {
                return (Response<R>) response;
            }

            return Response.success((R) result);

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

    // ── Batch Dispatch ─────────────────────────────────────────────────

    /**
     * Sends multiple requests sequentially and returns all responses.
     *
     * @param requests the requests to dispatch; must not be null
     * @return list of responses in the same order as the requests
     * @throws NullPointerException if {@code requests} is null
     */
    public List<Response<?>> sendAll(
            @NotNull List<? extends IRequest<?>> requests) {
        Objects.requireNonNull(requests, "requests must not be null");
        List<Response<?>> results = new ArrayList<>(requests.size());
        for (IRequest<?> request : requests) {
            results.add(sendWildcard(request));
        }
        return results;
    }

    /**
     * Sends multiple requests in parallel and returns all responses.
     *
     * @param requests the requests to dispatch; must not be null
     * @return future that completes with all responses in order
     * @throws NullPointerException if {@code requests} is null
     */
    public CompletableFuture<List<Response<?>>> sendAllAsync(
            @NotNull List<? extends IRequest<?>> requests) {
        Objects.requireNonNull(requests, "requests must not be null");
        Executor executor = asyncExecutor != null
                ? asyncExecutor : VIRTUAL_THREAD_EXECUTOR;
        CourierContext ctx = CourierContextHolder.peekContext();

        List<CompletableFuture<Response<?>>> futures =
                new ArrayList<>(requests.size());
        for (IRequest<?> request : requests) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                if (ctx != null) {
                    CourierContextHolder.setContext(ctx);
                }
                try {
                    return sendWildcard(request);
                } finally {
                    CourierContextHolder.clear();
                }
            }, executor));
        }

        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture<?>[0]));
        return allDone.thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()));
    }

    private <R> Response<R> sendWildcard(IRequest<R> request) {
        return send(request);
    }

    // ── Timeout Resolution ───────────────────────────────────────────────

    /**
     * Resolves the effective timeout for a request. If the request
     * class is annotated with {@link Timeout}, that value is used;
     * otherwise the global {@code asyncTimeoutMs} applies.
     */
    private long resolveTimeout(IRequest<?> request) {
        Timeout annotation = request.getClass()
                .getAnnotation(Timeout.class);
        if (annotation != null && annotation.value() > 0) {
            return annotation.value();
        }
        return asyncTimeoutMs;
    }

    // ── Notification Publishing ─────────────────────────────────────────

    /**
     * Publishes a notification to all registered handlers using the
     * configured {@link NotificationPublishingStrategy}.
     *
     * @param notification the notification to publish; must not be null
     * @throws NullPointerException if {@code notification} is null
     * @throws NotificationHandlerException under STOP_ON_FIRST_ERROR
     *         when a handler fails
     * @see #publishAsync(INotification) for non-blocking variant
     */
    public void publish(@NotNull INotification notification) {
        Objects.requireNonNull(notification,
                "notification must not be null");
        logger.debug("Publishing notification: {}",
                notification.getClass().getSimpleName());

        List<Object> handlers = notificationRegistry.getHandlers(
                notification.getClass());

        if (handlers.isEmpty()) {
            logger.warn("No handler registered for notification: {}",
                    notification.getClass().getSimpleName());
            return;
        }

        switch (notificationStrategy) {
            case SEQUENTIAL -> publishSequential(notification, handlers);
            case PARALLEL_WHEN_ALL ->
                    publishParallel(notification, handlers);
            case STOP_ON_FIRST_ERROR ->
                    publishStopOnFirstError(notification, handlers);
            default -> publishSequential(notification, handlers);
        }
    }

    private void publishSequential(INotification notification,
            List<Object> handlers) {
        for (Object handler : handlers) {
            invokeNotificationHandler(handler, notification, false);
        }
    }

    private void publishParallel(INotification notification,
            List<Object> handlers) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Executor executor = asyncExecutor != null
                ? asyncExecutor : VIRTUAL_THREAD_EXECUTOR;

        for (Object handler : handlers) {
            futures.add(CompletableFuture.runAsync(
                    () -> invokeNotificationHandler(
                            handler, notification, false),
                    executor));
        }

        CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new)).join();
    }

    private void publishStopOnFirstError(INotification notification,
            List<Object> handlers) {
        for (Object handler : handlers) {
            invokeNotificationHandler(handler, notification, true);
        }
    }

    private void invokeNotificationHandler(Object handler,
            INotification notification, boolean propagateErrors) {
        try {
            Method method = getCachedHandleMethod(handler.getClass());
            method.invoke(handler, notification);
            logger.debug("Notification handler executed: {} -> {}",
                    notification.getClass().getSimpleName(),
                    handler.getClass().getSimpleName());
        } catch (Exception e) {
            Throwable cause = e;
            if (e instanceof InvocationTargetException ite
                    && ite.getTargetException() != null) {
                cause = ite.getTargetException();
            }
            if (propagateErrors) {
                throw new NotificationHandlerException(
                        handler.getClass().getSimpleName(), cause);
            }
            logger.error("Error executing notification handler {}: {}",
                    handler.getClass().getSimpleName(),
                    cause.getMessage(), cause);
        }
    }

    /**
     * Publishes a notification asynchronously to all registered handlers.
     *
     * @param notification the notification to publish; must not be null
     * @return CompletableFuture that completes when all handlers finish
     * @throws NullPointerException if {@code notification} is null
     */
    public CompletableFuture<Void> publishAsync(
            @NotNull INotification notification) {
        Objects.requireNonNull(notification,
                "notification must not be null");
        Executor executor = asyncExecutor != null
                ? asyncExecutor : VIRTUAL_THREAD_EXECUTOR;
        return CompletableFuture.runAsync(
                () -> publish(notification), executor);
    }

    // ── Pre/Post Processors & Exception Handlers ────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <R> void runPreProcessors(IRequest<R> request) {
        if (processorRegistry == null) {
            return;
        }
        for (PreProcessor processor
                : processorRegistry.getPreProcessors()) {
            try {
                processor.process(request);
            } catch (Exception ex) {
                logger.error("PreProcessor {} failed for {}: {}",
                        processor.getClass().getSimpleName(),
                        request.getClass().getSimpleName(),
                        ex.getMessage(), ex);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <R> void runPostProcessors(
            IRequest<R> request, Response<R> response) {
        if (processorRegistry == null) {
            return;
        }
        for (PostProcessor processor
                : processorRegistry.getPostProcessors()) {
            try {
                processor.process(request, response);
            } catch (Exception ex) {
                logger.error("PostProcessor {} failed for {}: {}",
                        processor.getClass().getSimpleName(),
                        request.getClass().getSimpleName(),
                        ex.getMessage(), ex);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <R> Response<R> tryExceptionHandlers(
            IRequest<R> request, Exception exception) {
        if (processorRegistry == null) {
            return null;
        }
        for (IRequestExceptionHandler handler
                : processorRegistry.getExceptionHandlers()) {
            try {
                Object result = handler.handle(request, exception);
                if (result != null) {
                    logger.debug("Exception handled by {}: {}",
                            handler.getClass().getSimpleName(),
                            exception.getMessage());
                    return (Response<R>) result;
                }
            } catch (Exception ex) {
                logger.error("ExceptionHandler {} threw: {}",
                        handler.getClass().getSimpleName(),
                        ex.getMessage(), ex);
            }
        }
        return null;
    }

    // ── Notification method cache ───────────────────────────────────────

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
        public HandlerMethodNotFoundException(
                String handlerClassName, String expectedMethod) {
            super("No handle method (" + expectedMethod
                    + ") found in handler: " + handlerClassName);
        }
    }
}
