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
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dispatches requests through the CQRS pipeline, invoking synchronous or
 * asynchronous handlers as needed. Also supports publishing notifications
 * to multiple handlers.
 */
@Component
public class Courier {
    private static final Logger logger = LoggerFactory.getLogger(Courier.class);
    private final HandlerRegistry handlerRegistry;
    private final NotificationRegistry notificationRegistry;
    private final PipelineExecutor pipelineExecutor;

    public Courier(@NotNull HandlerRegistry handlerRegistry,
                   @NotNull NotificationRegistry notificationRegistry,
                   @NotNull PipelineExecutor pipelineExecutor) {
        this.handlerRegistry = handlerRegistry;
        this.notificationRegistry = notificationRegistry;
        this.pipelineExecutor = pipelineExecutor;
        logger.info("Courier initialized with {} registered handlers", handlerRegistry.getHandlerCount());
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
            Method method = findHandleOrExecuteMethod(handler.getClass());
            long start = System.currentTimeMillis();

            Object result = method.invoke(handler, request);

            if (result instanceof CompletableFuture<?> future) {
                result = future.join();
            }

            long duration = System.currentTimeMillis() - start;
            logger.debug("Handler executed in {}ms: {} -> {}", duration,
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
        } catch (Exception e) {
            logger.error("Failed to invoke handler: {}", e.getMessage(), e);
            return Response.error(e);
        }
    }

    @SuppressWarnings("java:S3011")
    private @NotNull Method findHandleOrExecuteMethod(@NotNull Class<?> handlerClass) {
        for (Method method : handlerClass.getMethods()) {
            if ((method.getName().equals("handle") || method.getName().equals("execute"))
                    && method.getParameterCount() == 1) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new HandlerMethodNotFoundException(handlerClass.getName(), "handle/execute");
    }

    /**
     * Publishes a notification to all registered handlers.
     * All handlers will be invoked, and any exceptions will be logged but not propagated.
     *
     * @param notification the notification to publish
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
                Method method = findHandleMethod(handler.getClass());
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
     * @param notification the notification to publish
     * @return CompletableFuture that completes when all handlers finish
     */
    public CompletableFuture<Void> publishAsync(@NotNull INotification notification) {
        return CompletableFuture.runAsync(() -> publish(notification));
    }

    @SuppressWarnings("java:S3011")
    private @NotNull Method findHandleMethod(@NotNull Class<?> handlerClass) {
        for (Method method : handlerClass.getMethods()) {
            if (method.getName().equals("handle") && method.getParameterCount() == 1) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new HandlerMethodNotFoundException(handlerClass.getName(), "handle");
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