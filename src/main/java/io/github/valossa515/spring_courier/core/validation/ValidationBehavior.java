package io.github.valossa515.spring_courier.core.validation;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline behavior that validates requests before they reach the handler.
 * This provides a centralized validation layer for the CQRS pipeline.
 *
 * <p><strong>Type constraint:</strong> the response type {@code R} <em>must</em> be
 * {@link Response Response&lt;?&gt;} (or a compatible subtype). When validation
 * fails, the behavior short-circuits the pipeline and returns a
 * {@link Response#error(String, int) Response.error(...)} with HTTP 400.
 * Using a different return type will cause a {@link ClassCastException} at
 * runtime.
 *
 * @param <T>  request type
 * @param <R> response type — must be {@code Response<?>}
 */
public class ValidationBehavior<T extends IRequest<R>, R>
        implements PipelineBehavior<T, R> {

    private static final Logger logger = LoggerFactory.getLogger(ValidationBehavior.class);
    private final List<Validator<T>> validators;

    public ValidationBehavior(List<Validator<T>> validators) {
        this.validators = validators != null ? validators : new ArrayList<>();
    }

    @Override
    public R handle(T request, Next<R> next) {
        logger.debug("Validating request: {}", request.getClass().getSimpleName());

        List<ValidationError> errors = new ArrayList<>();

        for (Validator<T> validator : validators) {
            try {
                ValidationResult result = validator.validate(request);
                if (!result.isValid()) {
                    errors.addAll(result.getErrors());
                }
            } catch (Exception e) {
                logger.error("Error during validation for request type: {}",
                        request.getClass().getSimpleName(), e);
                errors.add(new ValidationError("validation_error",
                        "Internal error during validation"));
            }
        }

        if (!errors.isEmpty()) {
            logger.warn("Validation failed for {}: {} errors found",
                    request.getClass().getSimpleName(), errors.size());
            return createValidationErrorResponse(errors);
        }

        return next.invoke();
    }

    /**
     * Builds a {@link Response#error(String, int)} with HTTP 400.
     *
     * @throws ClassCastException if {@code R} is not {@code Response<?>}
     */
    @SuppressWarnings("unchecked")
    private R createValidationErrorResponse(List<ValidationError> errors) {
        StringBuilder errorMessage = new StringBuilder("Validation errors: ");
        int maxErrors = Math.min(errors.size(), 50);
        for (int i = 0; i < maxErrors; i++) {
            ValidationError error = errors.get(i);
            errorMessage.append(sanitize(error.field()))
                    .append(": ")
                    .append(sanitize(error.message()))
                    .append("; ");
        }
        if (errors.size() > maxErrors) {
            errorMessage.append("... and ").append(errors.size() - maxErrors).append(" more errors");
        }

        // R must be Response<?> — see class-level Javadoc.
        // Due to type erasure, the unchecked cast cannot fail here; it will
        // surface as a CCE at the call-site when R is not Response<?>.
        return (R) Response.error(errorMessage.toString(), 400);
    }

    /**
     * Sanitizes user-controlled values to prevent log injection attacks.
     * Removes newlines, carriage returns, and other control characters
     * that could be used to forge log entries.
     */
    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        // Remove control characters (newlines, tabs, carriage returns, etc.)
        String sanitized = value.replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F]", "");
        // Truncate overly long values
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...";
        }
        return sanitized;
    }
}