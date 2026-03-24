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
 * <p>Activated automatically when {@code jakarta.validation.Validator} is
 * available on the classpath and a {@code Validator} bean exists in the
 * Spring context (e.g., via {@code spring-boot-starter-validation}).
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
