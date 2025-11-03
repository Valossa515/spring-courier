package io.github.valossa515.spring_courier.annotations;

import io.github.valossa515.spring_courier.config.CourierAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Anotação para habilitar a configuração automática do Spring Courier em projetos Spring Boot.
 * <p>
 * Ao ser aplicada em uma classe de configuração, importa o {@link io.github.valossa515.spring_courier.config.CourierAutoConfiguration},
 * permitindo o registro e funcionamento dos componentes necessários para o padrão CQRS.
 * </p>
 *
 * <p>Exemplo de uso:</p>
 * <pre>{@code
 * @EnableSpringCourier
 * @SpringBootApplication
 * public class MinhaAplicacao { ... }
 * }</pre>
 *
 * @see io.github.valossa515.spring_courier.config.CourierAutoConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CourierAutoConfiguration.class)
public @interface EnableSpringCourier {
}
