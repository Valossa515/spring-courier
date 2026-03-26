package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * Pipeline behavior that retries failed request executions with
 * exponential backoff.
 *
 * <p>When the downstream pipeline (or handler) throws an exception,
 * this behavior catches it and retries up to
 * {@code maxAttempts - 1} additional times, with a delay that
 * grows exponentially: {@code delayMs * multiplier^(attempt-1)}.
 *
 * <p>Enabled via {@code spring.courier.retry.enabled=true}
 * (disabled by default).
 *
 * @param <R> request type
 * @param <S> response type
 */
public class RetryBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(RetryBehavior.class);

    private final int maxAttempts;
    private final long delayMs;
    private final double multiplier;

    /**
     * Creates a retry behavior.
     *
     * @param maxAttempts maximum total attempts (including the first)
     * @param delayMs     initial delay between retries in milliseconds
     * @param multiplier  backoff multiplier (e.g. 2.0 for doubling)
     */
    public RetryBehavior(int maxAttempts, long delayMs,
            double multiplier) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.delayMs = Math.max(0, delayMs);
        this.multiplier = Math.max(1.0, multiplier);
    }

    @Override
    public S handle(R request, Next<S> next) {
        String requestName = request.getClass().getSimpleName();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return next.invoke();
            } catch (Exception ex) {
                lastException = ex;

                if (attempt >= maxAttempts) {
                    logger.error(
                            "All {} retry attempts exhausted "
                                    + "for {}: {}",
                            maxAttempts, requestName,
                            ex.getMessage());
                    break;
                }

                long currentDelay = computeDelay(attempt);
                logger.warn(
                        "Attempt {}/{} failed for {}, "
                                + "retrying in {}ms: {}",
                        attempt, maxAttempts, requestName,
                        currentDelay, ex.getMessage());

                sleep(currentDelay);
            }
        }

        throw asRuntimeException(lastException);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    long computeDelay(int attempt) {
        return (long) (delayMs * Math.pow(multiplier, (double) attempt - 1));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Retry interrupted", e);
        }
    }

    private RuntimeException asRuntimeException(Exception ex) {
        if (ex instanceof RuntimeException re) {
            return re;
        }
        return new RuntimeException(ex);
    }
}
