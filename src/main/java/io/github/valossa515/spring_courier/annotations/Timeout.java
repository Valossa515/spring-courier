package io.github.valossa515.spring_courier.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the global async timeout for a specific request type.
 *
 * <p>When placed on a class implementing {@code IRequest}, the
 * Courier dispatcher uses this value instead of the global
 * {@code spring.courier.async-timeout-ms} setting.
 *
 * <pre>{@code
 * @Timeout(5000)
 * public record SlowQuery() implements IQuery<String> {}
 * }</pre>
 *
 * @see io.github.valossa515.spring_courier.core.Courier
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timeout {

    /**
     * Timeout in milliseconds. Must be a positive value.
     *
     * @return timeout in milliseconds
     */
    long value();
}
