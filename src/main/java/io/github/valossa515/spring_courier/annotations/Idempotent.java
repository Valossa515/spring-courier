package io.github.valossa515.spring_courier.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a request class as idempotent. When the
 * {@code IdempotencyBehavior} is enabled, duplicate requests
 * with the same idempotency key will return the cached response
 * instead of re-executing the handler.
 *
 * <p>The idempotency key is derived from the request's
 * {@code toString()} representation by default (which works
 * out of the box with Java records). Override
 * {@code toString()} for custom key logic.
 *
 * <pre>{@code
 * @Idempotent(ttlSeconds = 3600)
 * public record CreateOrder(String orderId, BigDecimal amount)
 *         implements ICommand<OrderResult> {}
 * }</pre>
 *
 * @see io.github.valossa515.spring_courier.core.pipelines.IdempotencyBehavior
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Time-to-live in seconds for the cached response.
     * A value of {@code 0} means the entry never expires
     * (until evicted by max-size). Defaults to 3600 (1 hour).
     *
     * @return TTL in seconds
     */
    int ttlSeconds() default 3600;
}
