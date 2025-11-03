package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;

/**
 * Represents an intermediate behavior within the request processing pipeline.
 * Each implementation can execute logic before or after invoking the next
 * element in the chain, enabling cross-cutting features such as logging,
 * validation, or auditing.
 *
 * @param <TRequest>  request type, which must implement {@link IRequest} for the
 *                    corresponding response type.
 * @param <TResponse> response type expected for the handled request.
 */
public interface PipelineBehavior<TRequest extends IRequest<TResponse>, TResponse> {

    /**
     * Handles the current request and decides when (or if) the execution should
     * be delegated to the next element in the chain.
     *
     * @param request request received by the pipeline.
     * @param next    invoker responsible for calling the next behavior or the
     *                final handler.
     * @return response produced by the pipeline.
     */
    TResponse handle(TRequest request, Next<TResponse> next);

    /**
     * Contract that encapsulates the invocation of the next registered
     * behavior or the final handler. It is used to compose the pipeline
     * execution chain.
     *
     * @param <TResponse> type of the response expected after invoking the next
     *                    behavior.
     */
    interface Next<TResponse> {

        /**
         * Executes the next registered behavior or the final handler in the
         * pipeline.
         *
         * @return response produced by the continuation of the chain.
         */
        TResponse invoke();
    }
}
