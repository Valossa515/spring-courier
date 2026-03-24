package io.github.valossa515.spring_courier.core.validation;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Set;

/**
 * Bridges Jakarta Bean Validation ({@code jakarta.validation.Validator})
 * to the Courier {@link io.github.valossa515.spring_courier.core.validation.Validator}
 * contract, enabling automatic validation of requests annotated with
 * Jakarta constraints ({@code @NotNull}, {@code @Size}, {@code @Email}, etc.).
 *
 * <p>This bridge is <strong>not</strong> auto-configured. It must be
 * manually registered as a Spring bean by the application, for example
 * via a {@code @Bean} method that injects the Jakarta
 * {@code Validator} and passes it to the constructor.
 *
 * @param <T> request type to validate
 */
public class JakartaValidationBridge<T extends IRequest<?>>
        implements io.github.valossa515.spring_courier.core.validation.Validator<T> {

    private final Validator jakartaValidator;

    public JakartaValidationBridge(Validator jakartaValidator) {
        this.jakartaValidator = jakartaValidator;
    }

    @Override
    public ValidationResult validate(T request) {
        Set<ConstraintViolation<T>> violations = jakartaValidator.validate(request);

        if (violations.isEmpty()) {
            return ValidationResult.success();
        }

        List<ValidationError> errors = violations.stream()
                .map(v -> new ValidationError(
                        v.getPropertyPath().toString(),
                        v.getMessage()))
                .toList();

        return ValidationResult.failure(errors);
    }
}
