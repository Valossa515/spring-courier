package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Marker interface for CQRS queries that produce a response via {@link IRequest}.
 *
 * @param <R> response type
 */
public interface IQuery<R> extends IRequest<R> {
}
