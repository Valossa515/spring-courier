package io.github.valossa515.spring_courier.core.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    @Test
    void successFactoriesPopulateFields() {
        Response<String> withData = Response.success("value");
        assertTrue(withData.isSuccess());
        assertEquals("value", withData.getData());
        assertEquals(200, withData.getStatusCode());
        assertFalse(withData.hasError());
        assertTrue(withData.hasData());

        Response<String> withStatus = Response.success("value", 201);
        assertEquals(201, withStatus.getStatusCode());

        Response<String> withoutData = Response.success();
        assertTrue(withoutData.isSuccess());
        assertNull(withoutData.getData());
        assertEquals(200, withoutData.getStatusCode());
    }

    @Test
    void errorFactoriesPopulateFields() {
        Response<String> withMessage = Response.error("problem");
        assertFalse(withMessage.isSuccess());
        assertEquals("problem", withMessage.getError());
        assertEquals(500, withMessage.getStatusCode());
        assertTrue(withMessage.hasError());
        assertFalse(withMessage.hasData());

        Response<String> withStatus = Response.error("problem", 400);
        assertEquals(400, withStatus.getStatusCode());

        RuntimeException failure = new RuntimeException("boom");
        Response<String> fromThrowable = Response.error(failure);
        assertEquals("boom", fromThrowable.getError());
        assertEquals(500, fromThrowable.getStatusCode());

        Response<String> fromThrowableWithStatus = Response.error(failure, 503);
        assertEquals(503, fromThrowableWithStatus.getStatusCode());
    }

    @Test
    void getDataOrThrowBehavesAccordingToResponseState() {
        Response<String> success = Response.success("ok");
        assertEquals("ok", success.getDataOrThrow());

        Response<String> error = Response.error("problem");
        Response.ResponseException thrown =
                assertThrows(Response.ResponseException.class, error::getDataOrThrow);
        assertTrue(thrown.getMessage().contains("problem"));

        Response.ResponseException custom = new Response.ResponseException("custom", 499);
        Response<String> errorResponse = Response.error("another");
        Response.ResponseException customThrown =
                assertThrows(Response.ResponseException.class, () -> errorResponse.getDataOrThrow(custom));
        assertSame(custom, customThrown);
    }

    @Test
    void builderCreatesExpectedResponseAndSupportsEquality() {
        Response<String> built = Response.<String>builder()
                .data("data")
                .error("none")
                .success(true)
                .statusCode(204)
                .build();

        assertEquals("data", built.getData());
        assertEquals("none", built.getError());
        assertTrue(built.isSuccess());
        assertEquals(204, built.getStatusCode());

        Response<String> identical = Response.<String>builder()
                .data("data")
                .error("none")
                .success(true)
                .statusCode(204)
                .build();

        assertEquals(built, identical);
        assertEquals(built.hashCode(), identical.hashCode());
    }
}