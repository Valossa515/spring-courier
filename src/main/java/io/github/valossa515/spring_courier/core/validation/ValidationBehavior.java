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
 * @param <T>  request type
 * @param <R> response type
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
        logger.debug("Validando request: {}", request.getClass().getSimpleName());

        List<ValidationError> errors = new ArrayList<>();

        for (Validator<T> validator : validators) {
            try {
                ValidationResult result = validator.validate(request);
                if (!result.isValid()) {
                    errors.addAll(result.getErrors());
                }
            } catch (Exception e) {
                logger.error("Erro durante validação: {}", e.getMessage(), e);
                errors.add(new ValidationError("validation_error",
                        "Erro interno durante validação: " + e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            logger.warn("Validação falhou para {}: {} erros encontrados",
                    request.getClass().getSimpleName(), errors.size());
            return createValidationErrorResponse(errors);
        }

        return next.invoke();
    }

    @SuppressWarnings("unchecked")
    private R createValidationErrorResponse(List<ValidationError> errors) {
        StringBuilder errorMessage = new StringBuilder("Erros de validação: ");
        for (ValidationError error : errors) {
            errorMessage.append(error.field())
                    .append(": ")
                    .append(error.message())
                    .append("; ");
        }

        // If R is Response<TPayload>, return an error response
        return (R) Response.error(errorMessage.toString(), 400);
    }
}