package io.github.valossa515.spring_courier.core.validation;

/**
 * Contract for validators that check request validity.
 *
 * @param <TRequest> request type to validate
 */
public interface Validator<TRequest> {
    
    /**
     * Validates the request and returns the result.
     *
     * @param request the request to validate
     * @return validation result
     */
    ValidationResult validate(TRequest request);
}
