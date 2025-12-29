package io.github.valossa515.spring_courier.core.validation;

/**
 * Contract for validators that check request validity.
 *
 * @param <R> request type to validate
 */
public interface Validator<R> {
    
    /**
     * Validates the request and returns the result.
     *
     * @param request the request to validate
     * @return validation result
     */
    ValidationResult validate(R request);
}
