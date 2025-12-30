package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Functional interface for handling CQRS queries.
 *
 * @param <Q> query type that extends {@link IRequest}
 * @param <R> response type returned by the handler
 */
@FunctionalInterface
public interface QueryHandler<Q extends IRequest<R>, R> {
    /**
     * Handles the received query and returns the resulting response.
     *
     * @param query query to process
     * @return response produced by the handler
     */
    R handle(Q query);
}
