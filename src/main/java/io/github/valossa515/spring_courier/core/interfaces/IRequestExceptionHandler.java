package io.github.valossa515.spring_courier.core.interfaces;

import io.github.valossa515.spring_courier.core.support.Response;

/**
 * Pluggable exception handler for requests dispatched through the Courier
 * pipeline. Allows applications to provide custom error handling logic
 * (e.g., domain-specific error mapping, notification, recovery) instead
 * of relying on the default catch-all in {@code Courier}.
 *
 * <p>If a handler returns a non-null {@link Response}, it is used as the
 * result of the {@code send()} call. If it returns {@code null}, the
 * next registered exception handler is tried; if none handles the
 * exception, the default behavior applies.
 *
 * @param <T> request type
 * @param <R> response payload type
 * @param <E> exception type this handler can process
 */
public interface IRequestExceptionHandler<T extends IRequest<?>, R, E extends Exception> {

    /**
     * Attempts to handle the exception thrown during request processing.
     *
     * @param request   the original request
     * @param exception the exception thrown by the handler or pipeline
     * @return a {@link Response} to use as the result, or {@code null}
     *         to delegate to the next exception handler
     */
    Response<R> handle(T request, E exception);
}
