package io.github.valossa515.spring_courier.integration;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.support.Response;
import io.github.valossa515.spring_courier.core.validation.ValidationBehavior;
import io.github.valossa515.spring_courier.core.validation.ValidationError;
import io.github.valossa515.spring_courier.core.validation.ValidationResult;
import io.github.valossa515.spring_courier.core.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationIntegrationTest {

    @Test
    void validationBehaviorReturnsErrorWhenValidationFails() {
        TestCommand command = new TestCommand("");
        TestValidator validator = new TestValidator();
        ValidationBehavior<TestCommand, Response<String>> behavior = 
            new ValidationBehavior<>(List.of(validator));

        Response<String> result = behavior.handle(command, () -> Response.success("success"));

        // The result should be a Response error
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Name cannot be empty"));
    }

    @Test
    void validationBehaviorCallsNextWhenValidationSucceeds() {
        TestCommand command = new TestCommand("valid-name");
        TestValidator validator = new TestValidator();
        ValidationBehavior<TestCommand, Response<String>> behavior = 
            new ValidationBehavior<>(List.of(validator));

        Response<String> result = behavior.handle(command, () -> Response.success("success"));

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
    }

    @Test
    void validationResultCreatesSuccessResult() {
        ValidationResult result = ValidationResult.success();

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validationResultCreatesFailureWithErrors() {
        ValidationError error = new ValidationError("field", "error message");
        ValidationResult result = ValidationResult.failure(error);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("field", result.getErrors().get(0).field());
        assertEquals("error message", result.getErrors().get(0).message());
    }

    @Test
    void validatorCanAccumulateMultipleErrors() {
        TestCommand command = new TestCommand("");
        MultiErrorValidator validator = new MultiErrorValidator();

        ValidationResult result = validator.validate(command);

        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
    }

    // Test classes
    static class TestCommand implements ICommand<Response<String>> {
        private final String name;

        TestCommand(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class TestValidator implements Validator<TestCommand> {
        @Override
        public ValidationResult validate(TestCommand command) {
            if (command.getName() == null || command.getName().isEmpty()) {
                return ValidationResult.failure(
                    new ValidationError("name", "Name cannot be empty")
                );
            }
            return ValidationResult.success();
        }
    }

    static class MultiErrorValidator implements Validator<TestCommand> {
        @Override
        public ValidationResult validate(TestCommand command) {
            List<ValidationError> errors = new ArrayList<>();
            
            if (command.getName() == null || command.getName().isEmpty()) {
                errors.add(new ValidationError("name", "Name cannot be empty"));
            }
            
            if (command.getName() != null && command.getName().length() < 3) {
                errors.add(new ValidationError("name", "Name must be at least 3 characters"));
            }
            
            return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
        }
    }
}
