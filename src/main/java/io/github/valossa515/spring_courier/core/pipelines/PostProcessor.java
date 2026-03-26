package io.github.valossa515.spring_courier.core.pipelines;

/**
 * Simplified hook executed <em>after</em> the handler processes a request.
 * Unlike {@link PipelineBehavior}, a postprocessor receives the request
 * and the response but cannot modify the response — it is a pure
 * side-effect callback (auditing, metrics enrichment, cache invalidation, etc.).
 *
 * <p>Implementations are auto-discovered from the Spring context and
 * executed in {@link org.springframework.core.annotation.Order} order.
 *
 * @param <R> request type
 * @param <S> response type
 */
@FunctionalInterface
public interface PostProcessor<R, S> {

    /**
     * Invoked after the handler produces a response.
     *
     * @param request  the original request
     * @param response the response produced by the handler
     */
    void process(R request, S response);
}
