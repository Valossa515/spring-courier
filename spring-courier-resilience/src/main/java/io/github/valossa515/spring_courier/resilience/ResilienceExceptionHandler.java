package io.github.valossa515.spring_courier.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import io.github.valossa515.spring_courier.core.support.Response;

/**
 * Turns Resilience4j rejections into responses that say what actually
 * happened.
 *
 * <p>Without this, a rejected call surfaces as a generic {@code 500 "An
 * internal error occurred"}: core deliberately masks the message of any
 * exception that is not a {@code CourierException}, and that type is
 * {@code sealed}, so a module cannot add itself to the hierarchy. Mapping the
 * exceptions here — the extension point core provides for exactly this — keeps
 * the masking intact while still giving callers an actionable status:
 *
 * <ul>
 *   <li>circuit open &rarr; {@code 503 Service Unavailable}</li>
 *   <li>rate limit exhausted &rarr; {@code 429 Too Many Requests}</li>
 *   <li>bulkhead saturated &rarr; {@code 503 Service Unavailable}</li>
 * </ul>
 *
 * <p>Any other exception returns {@code null}, which passes it on to the next
 * handler and, ultimately, to the default Courier error handling.
 *
 * <p>These statuses are the retry-friendly ones: {@code 503} and {@code 429}
 * tell a caller the request may succeed later, which a masked {@code 500}
 * never does.
 */
public class ResilienceExceptionHandler
        implements IRequestExceptionHandler<Object, Response<?>> {

    @Override
    public Response<?> handle(Object request, Exception exception) {
        if (exception instanceof CallNotPermittedException) {
            return Response.error(
                    "Service temporarily unavailable: the circuit is open for this operation",
                    503);
        }
        if (exception instanceof RequestNotPermitted) {
            return Response.error(
                    "Too many requests: the rate limit for this operation is exhausted",
                    429);
        }
        if (exception instanceof BulkheadFullException) {
            return Response.error(
                    "Service busy: too many concurrent executions of this operation",
                    503);
        }
        // Not ours — let the next handler (or core) deal with it.
        return null;
    }
}
