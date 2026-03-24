package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * Built-in pipeline behavior that provides structured logging for every
 * request dispatched through the Courier pipeline.
 *
 * <p>Logs the request type, category (command/query/request), execution
 * duration, and outcome (success/error) at {@code INFO} level. Errors
 * are logged at {@code WARN} level with the error message.
 *
 * <p>Activated via {@code spring.courier.logging.enabled=true} (default
 * {@code false}). Runs at {@link Ordered#HIGHEST_PRECEDENCE} so it wraps
 * all other behaviors.
 *
 * <p>Uses raw {@code IRequest} to avoid constraining the response type,
 * ensuring compatibility with handlers that return any type {@code S}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class LoggingBehavior
        implements PipelineBehavior<IRequest, Object>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(LoggingBehavior.class);

    @Override
    public Object handle(IRequest request, Next<Object> next) {
        String requestName = request.getClass().getSimpleName();
        String category = resolveCategory(request);

        logger.info("[Courier] Dispatching {} [{}]",
                requestName, category);

        long start = System.nanoTime();
        try {
            Object result = next.invoke();
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            if (result instanceof Response<?> response) {
                if (response.isSuccess()) {
                    logger.info(
                            "[Courier] {} completed in {}ms [status={}]",
                            requestName, durationMs,
                            response.getStatusCode());
                } else {
                    logger.warn(
                            "[Courier] {} failed in {}ms "
                                    + "[status={}, error={}]",
                            requestName, durationMs,
                            response.getStatusCode(),
                            response.getError());
                }
            } else {
                logger.info("[Courier] {} completed in {}ms",
                        requestName, durationMs);
            }

            return result;
        } catch (RuntimeException ex) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            logger.error("[Courier] {} threw exception in {}ms: {}",
                    requestName, durationMs, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveCategory(IRequest<?> request) {
        if (request instanceof ICommand<?>) {
            return "command";
        }
        if (request instanceof IQuery<?>) {
            return "query";
        }
        return "request";
    }
}
