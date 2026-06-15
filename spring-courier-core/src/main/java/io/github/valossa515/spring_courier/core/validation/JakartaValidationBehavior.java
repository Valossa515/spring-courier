package io.github.valossa515.spring_courier.core.validation;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.support.Response;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pipeline behavior that bridges Jakarta Bean Validation (JSR 380) with
 * the Courier pipeline. Automatically validates request objects using
 * {@code @NotNull}, {@code @Size}, {@code @Email}, and other standard
 * constraint annotations.
 *
 * <p>Activated when {@code jakarta.validation-api} and a validation
 * provider (e.g. Hibernate Validator) are on the classpath and
 * {@code spring.courier.validation.enabled=true} (the default).
 *
 * <p>Runs at order {@code Ordered.HIGHEST_PRECEDENCE + 100}, just after
 * {@code LoggingBehavior} but before user behaviors.
 *
 * @param <R> request type
 * @param <S> response type — should be {@code Response<?>} for
 *            short-circuit to work
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class JakartaValidationBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(JakartaValidationBehavior.class);

    private final Validator validator;

    public JakartaValidationBehavior(Validator validator) {
        this.validator = validator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S handle(R request, Next<S> next) {
        Set<ConstraintViolation<R>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": "
                            + v.getMessage())
                    .collect(Collectors.joining("; ",
                            "Validation errors: ", ""));

            logger.warn("Jakarta validation failed for {}: "
                            + "{} violation(s)",
                    request.getClass().getSimpleName(),
                    violations.size());

            return (S) Response.validationError(errorMessage, 400);
        }

        return next.invoke();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
