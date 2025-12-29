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
}