package io.github.valossa515.spring_courier.core.validation;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationBehaviorTest {

    @Test
    void returns400ResponseWhenValidationFails() {
        ValidationError error = new ValidationError("field", "must not be null");
        Validator<TestCommand> validator = request -> ValidationResult.failure(error);
        ValidationBehavior<TestCommand, Response<String>> behavior = new ValidationBehavior<>(List.of(validator));

        Response<String> result = behavior.handle(new TestCommand("value"), () -> Response.success("ok"));

        assertFalse(result.isSuccess());
        assertEquals(400, result.getStatusCode());
        assertTrue(result.getError().contains("field"));
        assertTrue(result.getError().contains("must not be null"));
    }

    @Test
    void returns400ResponseWhenValidatorThrows() {
        Validator<TestCommand> validator = request -> { throw new IllegalStateException("validator failed"); };
        ValidationBehavior<TestCommand, Response<String>> behavior = new ValidationBehavior<>(List.of(validator));

        Response<String> result = behavior.handle(new TestCommand("value"), () -> Response.success("ok"));

        assertFalse(result.isSuccess());
        assertEquals(400, result.getStatusCode());
        assertTrue(result.getError().contains("validation_error"));
        assertTrue(result.getError().contains("validator failed"));
    }

    private record TestCommand(String value) implements ICommand<Response<String>> { }
}