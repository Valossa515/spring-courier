package io.github.valossa515.spring_courier.annotations;

import io.github.valossa515.spring_courier.config.CourierAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables the Spring Courier autoconfiguration in a Spring Boot application
 * by importing {@link io.github.valossa515.spring_courier.config.CourierAutoConfiguration}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CourierAutoConfiguration.class)
public @interface EnableSpringCourier {
}
