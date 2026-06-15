package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Functional interface for handling CQRS commands.
 *
 * @param <C> command type that extends {@link IRequest}
 * @param <R> response type returned by the handler
 */
@FunctionalInterface
public interface CommandHandler<C extends IRequest<R>, R> {
    /**
     * Handles the received command and returns the resulting response.
     *
     * @param command command to process
     * @return response produced by the handler
     */
    R handle(C command);
}
