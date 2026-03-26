package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;

/**
 * Simplified hook executed <em>before</em> the handler processes a request.
 * Unlike {@link PipelineBehavior}, a preprocessor cannot short-circuit the
 * pipeline or modify the response — it is a pure side-effect callback
 * (logging, enrichment, context propagation, etc.).
 *
 * <p>Implementations are auto-discovered from the Spring context
 * and executed in registration order.
 *
 * @param <R> request type
 */
@FunctionalInterface
public interface PreProcessor<R extends IRequest<?>> {

    /**
     * Invoked before the handler processes the request.
     *
     * @param request the incoming request
     */
    void process(R request);
}
