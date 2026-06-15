package io.github.valossa515.spring_courier.core.interfaces;

import io.github.valossa515.spring_courier.core.support.Response;

/**
 * Strategy interface for handling exceptions thrown during request
 * processing. Implementations are auto-discovered from the Spring
 * context and invoked when a handler or pipeline behavior throws an
 * unhandled exception.
 *
 * <p>Multiple handlers can be registered; the first one that returns a
 * non-{@code null} {@link Response} wins and short-circuits the
 * remaining handlers. If no handler handles the exception, the default
 * Courier error handling applies.
 *
 * <p>Example:
 * <pre>{@code
 * @Component
 * public class BusinessExceptionHandler
 *         implements IRequestExceptionHandler<MyCommand, Response<String>> {
 *
 *     @Override
 *     public Response<String> handle(MyCommand request, Exception exception) {
 *         if (exception instanceof BusinessException bex) {
 *             return Response.error(bex.getMessage(), 422);
 *         }
 *         return null; // delegate to next handler
 *     }
 * }
 * }</pre>
 *
 * @param <R> request type
 * @param <S> response type
 */
@FunctionalInterface
public interface IRequestExceptionHandler<R, S> {

    /**
     * Attempts to handle the exception thrown while processing the request.
     *
     * @param request   the request that caused the exception
     * @param exception the exception thrown
     * @return a {@link Response} to use instead of the default error handling,
     *         or {@code null} to pass to the next exception handler
     */
    S handle(R request, Exception exception);
}
