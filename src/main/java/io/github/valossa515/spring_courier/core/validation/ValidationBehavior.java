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
                logger.error("Error during validation: {}", e.getMessage(), e);
                errors.add(new ValidationError("validation_error",
                        "Internal error during validation: " + e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            logger.warn("Validation failed for {}: {} errors found",
                    request.getClass().getSimpleName(), errors.size());
            return createValidationErrorResponse(errors);
        }

        return next.invoke();
    }

    /**\n     * Builds a {@link Response#error(String, int)} with HTTP 400.\n     *\n     * @throws ClassCastException if {@code R} is not {@code Response<?>}\n     */
    @SuppressWarnings("unchecked")
    private R createValidationErrorResponse(List<ValidationError> errors) {
        StringBuilder errorMessage = new StringBuilder("Validation errors: ");
        for (ValidationError error : errors) {
            errorMessage.append(error.field())
                    .append(": ")
                    .append(error.message())
                    .append("; ");
        }

        // R must be Response<?> — see class-level Javadoc.
        // Due to type erasure, the unchecked cast cannot fail here; it will
        // surface as a CCE at the call-site when R is not Response<?>.
        return (R) Response.error(errorMessage.toString(), 400);
    }
}