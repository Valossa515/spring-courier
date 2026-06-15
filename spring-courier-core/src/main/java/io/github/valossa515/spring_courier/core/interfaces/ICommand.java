package io.github.valossa515.spring_courier.core.interfaces;
/**
 * Marker interface for CQRS commands that produce a response via {@link IRequest}.
 *
 * @param <R> response type
 */
public interface ICommand<R> extends IRequest<R> {
}
