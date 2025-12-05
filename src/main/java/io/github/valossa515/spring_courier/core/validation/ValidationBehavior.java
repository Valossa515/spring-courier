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
 * @param <TRequest>  request type
 * @param <TResponse> response type
 */
public class ValidationBehavior<TRequest extends IRequest<TResponse>, TResponse> 
        implements PipelineBehavior<TRequest, TResponse> {

    private static final Logger logger = LoggerFactory.getLogger(ValidationBehavior.class);
    private final List<Validator<TRequest>> validators;

    public ValidationBehavior(List<Validator<TRequest>> validators) {
        this.validators = validators != null ? validators : new ArrayList<>();
    }

    @Override
    public TResponse handle(TRequest request, Next<TResponse> next) {
        logger.debug("Validando request: {}", request.getClass().getSimpleName());

        List<ValidationError> errors = new ArrayList<>();
        
        for (Validator<TRequest> validator : validators) {
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
    private TResponse createValidationErrorResponse(List<ValidationError> errors) {
        StringBuilder errorMessage = new StringBuilder("Erros de validação: ");
        for (ValidationError error : errors) {
            errorMessage.append(error.getField())
                    .append(": ")
                    .append(error.getMessage())
                    .append("; ");
        }
        
        // If TResponse is Response<T>, return an error response
        return (TResponse) Response.error(errorMessage.toString(), 400);
    }
}
