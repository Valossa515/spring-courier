package io.github.valossa515.spring_courier.core.interfaces;

import io.github.valossa515.spring_courier.core.support.Response;

/**
 * Hook executed <em>after</em> the handler produces a response.
 * Unlike a full {@link io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior},
 * a post-processor does not control the pipeline chain — it simply runs
 * post-processing logic (e.g., audit trail, enrichment, caching).
 *
 * <p>Multiple post-processors may be registered for the same request type
 * and are executed in {@link org.springframework.core.annotation.Order} order.
 *
 * @param <T> request type
 * @param <R> response payload type
 */
public interface IRequestPostProcessor<T extends IRequest<?>, R> {

    /**
     * Processes the response after the handler has been invoked.
     *
     * @param request  the original request
     * @param response the response produced by the handler
     */
    void process(T request, Response<R> response);
}
