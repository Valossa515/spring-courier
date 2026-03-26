package io.github.valossa515.spring_courier.core.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseErrorDetailsTest {

    record FieldError(String field, String message) {
    }

    @Test
    void errorWithDetailsCarriesPayload() {
        List<FieldError> errors = List.of(
                new FieldError("name", "must not be blank"),
                new FieldError("email", "invalid format"));

        Response<String> response = Response.errorWithDetails(
                "Validation failed", 400, errors);

        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertEquals("Validation failed", response.getError());
        assertTrue(response.hasErrorDetails());

        List<FieldError> retrieved = response.getErrorDetails();
        assertEquals(2, retrieved.size());
        assertEquals("name", retrieved.get(0).field());
    }

    @Test
    void errorWithoutDetailsHasNullDetails() {
        Response<String> response = Response.error("boom", 500);
        assertNull(response.getErrorDetails());
        assertFalse(response.hasErrorDetails());
    }

    @Test
    void successResponseHasNoErrorDetails() {
        Response<String> response = Response.success("ok");
        assertNull(response.getErrorDetails());
        assertFalse(response.hasErrorDetails());
    }

    @Test
    void validationErrorWithDetailsCarriesPayload() {
        Map<String, String> fieldErrors =
                Map.of("age", "must be positive");

        Response<String> response =
                Response.validationErrorWithDetails(
                        "Invalid input", 422, fieldErrors);

        assertFalse(response.isSuccess());
        assertTrue(response.isValidationFailure());
        assertEquals(422, response.getStatusCode());
        assertTrue(response.hasErrorDetails());

        Map<String, String> details =
                response.getErrorDetails();
        assertEquals("must be positive", details.get("age"));
    }

    @Test
    void builderSupportsErrorDetails() {
        Response<String> response = Response.<String>builder()
                .success(false)
                .error("bad request")
                .statusCode(400)
                .errorDetails(List.of("field1", "field2"))
                .build();

        assertFalse(response.isSuccess());
        assertTrue(response.hasErrorDetails());

        List<String> details = response.getErrorDetails();
        assertEquals(2, details.size());
    }

    @Test
    void errorDetailsWithCustomType() {
        record ProblemDetail(String type, String title,
                int status, String detail) {
        }

        ProblemDetail problem = new ProblemDetail(
                "about:blank", "Bad Request", 400,
                "The request body is malformed");

        Response<Void> response = Response.errorWithDetails(
                "Bad Request", 400, problem);

        ProblemDetail retrieved = response.getErrorDetails();
        assertNotNull(retrieved);
        assertEquals("Bad Request", retrieved.title());
        assertEquals(400, retrieved.status());
    }
}
