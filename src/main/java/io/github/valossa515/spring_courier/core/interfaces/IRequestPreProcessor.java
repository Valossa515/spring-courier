package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Hook executed <em>before</em> the handler processes the request.
 * Unlike a full {@link io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior},
 * a pre-processor does not control the pipeline chain — it simply runs
 * preparatory logic (e.g., enrichment, audit trail, logging).
 *
 * <p>Multiple pre-processors may be registered for the same request type
 * and are executed in {@link org.springframework.core.annotation.Order} order.
 *
 * @param <T> request type
 */
public interface IRequestPreProcessor<T extends IRequest<?>> {

    /**
     * Processes the request before the handler is invoked.
     *
     * @param request the incoming request
     */
    void process(T request);
}
