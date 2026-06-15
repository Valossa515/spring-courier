package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.validation.JakartaValidationBehavior;
import jakarta.validation.Validator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that registers the Jakarta Bean Validation bridge
 * when {@code jakarta.validation-api} and a validation provider are on
 * the classpath.
 *
 * <p>Can be disabled with {@code spring.courier.validation.enabled=false}.
 */
@Configuration
@ConditionalOnClass(Validator.class)
@ConditionalOnBean(Validator.class)
@ConditionalOnProperty(name = "spring.courier.validation.enabled",
        havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(CourierAutoConfiguration.class)
public class CourierValidationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JakartaValidationBehavior.class)
    public JakartaValidationBehavior<?, ?> jakartaValidationBehavior(
            Validator validator) {
        return new JakartaValidationBehavior<>(validator);
    }
}
