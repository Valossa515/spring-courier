package io.github.valossa515.spring_courier.core.metrics;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline behavior that records Micrometer metrics for every request
 * passing through the CQRS pipeline.
 *
 * <p>This behavior should typically run with highest priority (lowest order)
 * so that the timer wraps the entire pipeline execution including other
 * behaviors like validation.
 *
 * @param <T> request type
 * @param <R> response type
 */
public class MetricsBehavior<T extends IRequest<R>, R>
        implements PipelineBehavior<T, R> {

    private static final Logger logger = LoggerFactory.getLogger(MetricsBehavior.class);

    private final CourierMetrics metrics;

    public MetricsBehavior(CourierMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public R handle(T request, Next<R> next) {
        String requestType = request.getClass().getSimpleName();
        long start = System.nanoTime();

        try {
            R result = next.invoke();
            long durationNs = System.nanoTime() - start;

            if (isErrorResponse(result)) {
                metrics.recordError(requestType, durationNs, "ResponseError");
            } else {
                metrics.recordSuccess(requestType, durationNs);
            }

            logger.debug("Metrics recorded for {}: {}ns", requestType, durationNs);
            return result;

        } catch (Exception e) {
            long durationNs = System.nanoTime() - start;
            metrics.recordError(requestType, durationNs, e.getClass().getSimpleName());
            throw e;
        }
    }

    private boolean isErrorResponse(Object result) {
        if (result instanceof Response<?> response) {
            return !response.isSuccess();
        }
        return false;
    }
}
