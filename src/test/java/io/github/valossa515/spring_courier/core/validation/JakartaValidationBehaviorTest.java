package io.github.valossa515.spring_courier.core.validation;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.support.Response;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JakartaValidationBehaviorTest {

    record TestCmd(String name) implements ICommand<String> {
    }

    @Test
    @SuppressWarnings("unchecked")
    void validRequestPassesThrough() {
        Validator validator = mock(Validator.class);
        when(validator.validate(org.mockito.Mockito.any()))
                .thenReturn(Collections.emptySet());

        JakartaValidationBehavior<IRequest<Response<?>>,
                Response<?>> behavior =
                new JakartaValidationBehavior<>(validator);

        TestCmd cmd = new TestCmd("valid");
        PipelineBehavior.Next<Response<?>> next =
                () -> Response.success("ok");

        Response<?> result = behavior.handle(
                (IRequest<Response<?>>) (IRequest<?>) cmd, next);

        assertTrue(result.isSuccess());
        assertEquals("ok", result.getData());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void invalidRequestReturnsValidationError() {
        Validator validator = mock(Validator.class);

        ConstraintViolation violation =
                mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        when(validator.validate(org.mockito.Mockito.any()))
                .thenReturn(Set.of(violation));

        JakartaValidationBehavior<IRequest<Response<?>>,
                Response<?>> behavior =
                new JakartaValidationBehavior<>(validator);

        TestCmd cmd = new TestCmd(null);
        PipelineBehavior.Next<Response<?>> next =
                () -> Response.success("should not reach");

        Response<?> result = behavior.handle(
                (IRequest<Response<?>>) (IRequest<?>) cmd, next);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getStatusCode());
        assertTrue(result.getError()
                .contains("name: must not be blank"));
    }

    @Test
    void orderIsHighestPrecedencePlus100() {
        Validator validator = mock(Validator.class);
        JakartaValidationBehavior<?, ?> behavior =
                new JakartaValidationBehavior<>(validator);
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 100,
                behavior.getOrder());
    }
}
