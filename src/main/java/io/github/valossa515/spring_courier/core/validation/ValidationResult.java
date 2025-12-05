package io.github.valossa515.spring_courier.core.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of a validation operation.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<ValidationError> errors;

    private ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    /**
     * Creates a successful validation result.
     *
     * @return successful validation result
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Creates a failed validation result with errors.
     *
     * @param errors validation errors
     * @return failed validation result
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        return new ValidationResult(false, errors);
    }

    /**
     * Creates a failed validation result with a single error.
     *
     * @param error validation error
     * @return failed validation result
     */
    public static ValidationResult failure(ValidationError error) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(false, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }
}
