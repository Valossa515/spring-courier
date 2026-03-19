package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.exceptions.CourierException;
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
        assertEquals("An internal error occurred", fromThrowable.getError());
        assertEquals(500, fromThrowable.getStatusCode());

        Response<String> fromThrowableWithStatus = Response.error(failure, 503);
        assertEquals(503, fromThrowableWithStatus.getStatusCode());
        assertEquals("An internal error occurred", fromThrowableWithStatus.getError());
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
                .success(true)
                .statusCode(204)
                .build();

        assertEquals("data", built.getData());
        assertNull(built.getError());
        assertTrue(built.isSuccess());
        assertEquals(204, built.getStatusCode());

        Response<String> identical = Response.<String>builder()
                .data("data")
                .success(true)
                .statusCode(204)
                .build();

        assertEquals(built, identical);
        assertEquals(built.hashCode(), identical.hashCode());
    }

    @Test
    void toEntityBuildsResponseEntity() {
        Response<String> resp = Response.success("ok", 201);
        var entity = resp.toEntity();
        assertEquals(201, entity.getStatusCode().value());
        assertSame(resp, entity.getBody());
    }

    @Test
    void equalsHandlesNullAndDifferentTypes() {
        Response<String> base = Response.success("data");

        assertNotEquals(null, base);
        assertNotEquals(new Object(), base);
    }

    @Test
    void equalsDetectsFieldDifferences() {
        Response<String> base = Response.success("data", 201);
        Response<String> differentData = Response.success("other", 201);
        Response<String> differentStatus = Response.success("data", 202);
        Response<String> differentSuccess = Response.error("err", 201);
        Response<String> differentError = Response.error("different");

        assertNotEquals(base, differentData);
        assertNotEquals(base, differentStatus);
        assertNotEquals(base, differentSuccess);
        assertNotEquals(Response.success("data"), differentError);
    }

    @Test
    void equalsIsReflexiveAndSymmetric() {
        Response<String> base = Response.success("data", 201);
        Response<String> same = Response.success("data", 201);

        assertEquals(base, base);
        assertEquals(base, same);
        assertEquals(same, base);
    }

    @Test
    void responseExceptionExposesStatusCode() {
        Response.ResponseException ex = new Response.ResponseException("boom", 418);
        assertEquals(418, ex.getStatusCode());
    }

    @Test
    void errorFromCourierExceptionPassesMessageThrough() {
        CourierException ce = new CourierException("courier-specific error");
        Response<String> resp = Response.error(ce);
        assertEquals("courier-specific error", resp.getError());
        assertEquals(500, resp.getStatusCode());
        assertEquals("CourierException", resp.getExceptionType());

        Response<String> respWithStatus = Response.error(ce, 422);
        assertEquals("courier-specific error", respWithStatus.getError());
        assertEquals(422, respWithStatus.getStatusCode());
        assertEquals("CourierException", respWithStatus.getExceptionType());
    }

    @Test
    void errorFromNonCourierExceptionReturnsSanitizedMessage() {
        RuntimeException re = new RuntimeException("sensitive internal detail");
        Response<String> resp = Response.error(re);
        assertEquals("An internal error occurred", resp.getError());
        assertEquals("RuntimeException", resp.getExceptionType());

        Response<String> respWithStatus = Response.error(re, 502);
        assertEquals("An internal error occurred", respWithStatus.getError());
        assertEquals("RuntimeException", respWithStatus.getExceptionType());
    }

    @Test
    void errorFromThrowablePreservesExceptionType() {
        IllegalArgumentException iae = new IllegalArgumentException("bad arg");
        Response<String> resp = Response.error(iae);
        assertEquals("IllegalArgumentException", resp.getExceptionType());

        Response<String> respWithStatus = Response.error(iae, 400);
        assertEquals("IllegalArgumentException", respWithStatus.getExceptionType());
    }

    @Test
    void exceptionTypeIsNullForStringErrorFactories() {
        Response<String> withMessage = Response.error("problem");
        assertNull(withMessage.getExceptionType());

        Response<String> withStatus = Response.error("problem", 400);
        assertNull(withStatus.getExceptionType());
    }

    @Test
    void errorWithExplicitExceptionTypePreservesIt() {
        Response<String> resp = Response.error("timeout", 504, "TimeoutException");
        assertFalse(resp.isSuccess());
        assertEquals("timeout", resp.getError());
        assertEquals(504, resp.getStatusCode());
        assertEquals("TimeoutException", resp.getExceptionType());
    }

    @Test
    void validationErrorCarriesExceptionType() {
        Response<String> resp = Response.validationError("field: must not be blank", 400);
        assertFalse(resp.isSuccess());
        assertTrue(resp.isValidationFailure());
        assertEquals("ValidationException", resp.getExceptionType());
    }

    @Test
    void exceptionTypeIsNullForSuccessFactories() {
        Response<String> resp = Response.success("data");
        assertNull(resp.getExceptionType());
    }

    @Test
    void equalityIgnoresExceptionType() {
        Response<String> fromRuntime = Response.error(new RuntimeException("a"));
        Response<String> fromIllegal = Response.error(new IllegalArgumentException("b"));

        // Both are error responses with generic message and 500 status
        assertEquals(fromRuntime, fromIllegal,
                "exceptionType is diagnostic and should not affect equality");
    }

    @Test
    void anonymousExceptionTypeResolvesToUnknown() {
        // Anonymous class has empty getSimpleName()
        RuntimeException anonymous = new RuntimeException("anon") { };
        Response<String> resp = Response.error(anonymous);
        assertEquals("unknown", resp.getExceptionType(),
                "Empty simpleName from anonymous class should fall back to unknown");
    }

    @Test
    void toStringMasksDataField() {
        Response<String> withData = Response.success("secret-token-123");
        String str = withData.toString();
        assertFalse(str.contains("secret-token-123"));
        assertTrue(str.contains("<present>"));

        Response<String> withoutData = Response.success();
        String strNull = withoutData.toString();
        assertTrue(strNull.contains("data=null"));
    }

    @Test
    void builderRejectsSuccessWithErrorMessage() {
        assertThrows(IllegalStateException.class, () ->
                Response.<String>builder()
                        .success(true)
                        .error("oops")
                        .build());
    }

    @Test
    void builderRejectsErrorWithSuccessStatusCode() {
        assertThrows(IllegalStateException.class, () ->
                Response.<String>builder()
                        .success(false)
                        .error("fail")
                        .statusCode(200)
                        .build());

        assertThrows(IllegalStateException.class, () ->
                Response.<String>builder()
                        .success(false)
                        .error("fail")
                        .statusCode(299)
                        .build());
    }

    @Test
    void builderAllowsErrorWithNon2xxStatusCode() {
        Response<String> resp = Response.<String>builder()
                .success(false)
                .error("fail")
                .statusCode(400)
                .build();
        assertFalse(resp.isSuccess());
        assertEquals("fail", resp.getError());
        assertEquals(400, resp.getStatusCode());
    }
}