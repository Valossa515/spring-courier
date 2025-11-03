package io.github.valossa515.spring_courier.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Marks a handler implementation so that Spring registers it as a bean that
 * participates in the CQRS pipeline.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ExposeHandler {
    /**
     * Optional bean name to register for the handler.
     *
     * @return the bean name
     */
    String value() default "";
}
