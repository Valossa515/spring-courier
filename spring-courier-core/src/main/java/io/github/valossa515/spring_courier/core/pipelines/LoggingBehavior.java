package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.util.concurrent.TimeUnit;

/**
 * Built-in pipeline behavior that logs request entry, exit, duration,
 * and outcome for every dispatched command and query.
 *
 * <p>Enabled by default via autoconfiguration. Can be disabled with
 * {@code spring.courier.logging.enabled=false}.
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so it wraps all other
 * behaviors, giving an accurate total-duration measurement.
 *
 * @param <R> request type
 * @param <S> response type
 */
public class LoggingBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(LoggingBehavior.class);

    @Override
    public S handle(R request, Next<S> next) {
        String requestName = request.getClass().getSimpleName();
        logger.info("[Courier] --> Handling {}", requestName);
        long startNanos = System.nanoTime();

        try {
            S result = next.invoke();

            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startNanos);

            if (result instanceof Response<?> response) {
                if (response.isSuccess()) {
                    logger.info("[Courier] <-- {} completed in {}ms (status={})",
                            requestName, durationMs, response.getStatusCode());
                } else {
                    logger.warn("[Courier] <-- {} failed in {}ms (status={}, error={})",
                            requestName, durationMs,
                            response.getStatusCode(), response.getError());
                }
            } else {
                logger.info("[Courier] <-- {} completed in {}ms",
                        requestName, durationMs);
            }

            return result;

        } catch (Exception ex) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startNanos);
            logger.error("[Courier] <-- {} threw {} after {}ms: {}",
                    requestName, ex.getClass().getSimpleName(),
                    durationMs, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
