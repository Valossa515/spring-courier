package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.exceptions.CourierException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseCoverageTest {

    @Test
    void getDataOrThrowWithExceptionDoesNothingOnSuccess() {
        Response<String> success = Response.success("ok");
        Response.ResponseException ex = new Response.ResponseException("err", 500);
        assertDoesNotThrow(() -> success.getDataOrThrow(ex));
    }

    @Test
    void errorFromCourierExceptionInSingleArgFactory() {
        CourierException ce = new CourierException("courier msg");
        Response<String> resp = Response.error(ce);
        assertEquals("courier msg", resp.getError());
    }

    @Test
    void errorFromCourierExceptionInTwoArgFactory() {
        CourierException ce = new CourierException("courier msg");
        Response<String> resp = Response.error(ce, 422);
        assertEquals("courier msg", resp.getError());
        assertEquals(422, resp.getStatusCode());
    }

    @Test
    void toStringContainsAllFields() {
        Response<String> withData = Response.success("secret", 201);
        String str = withData.toString();
        assertTrue(str.contains("success=true"));
        assertTrue(str.contains("statusCode=201"));
        assertTrue(str.contains("<present>"));
        assertFalse(str.contains("secret"));

        Response<String> nullData = Response.success();
        String strNull = nullData.toString();
        assertTrue(strNull.contains("data=null"));
        assertTrue(strNull.contains("error='null'"));
    }

    @Test
    void equalsReturnsFalseWhenOnlyErrorDiffers() {
        Response<String> a = Response.<String>builder()
                .success(false).error("err-a").statusCode(400).build();
        Response<String> b = Response.<String>builder()
                .success(false).error("err-b").statusCode(400).build();
        assertNotEquals(a, b);
    }

    @SuppressWarnings("all")
    @Test
    void equalsReturnsFalseForNullAndDifferentClass() {
        Response<String> r = Response.success("x");
        // Must call equals directly — assertNotEquals(null, r) uses Objects.equals
        // which does NOT call r.equals(null)
        assertFalse(r.equals(null));
        assertFalse(r.equals("string"));
    }

    @Test
    void equalsReturnsTrueForIdenticalErrorResponses() {
        Response<String> a = Response.<String>builder()
                .success(false).error("same").statusCode(400).build();
        Response<String> b = Response.<String>builder()
                .success(false).error("same").statusCode(400).build();
        assertEquals(a, b);
    }

    @Test
    void builderAllowsSuccessWithNoError() {
        Response<String> resp = Response.<String>builder()
                .data("value")
                .success(true)
                .statusCode(200)
                .build();
        assertTrue(resp.isSuccess());
        assertEquals("value", resp.getData());
    }

    @Test
    void builderRejectsErrorWith2xxStatusCodes() {
        assertThrows(IllegalStateException.class, () ->
                Response.<String>builder()
                        .success(false).error("e").statusCode(200).build());
        assertThrows(IllegalStateException.class, () ->
                Response.<String>builder()
                        .success(false).error("e").statusCode(250).build());
        assertThrows(IllegalStateException.class, () ->
                Response.<String>builder()
                        .success(false).error("e").statusCode(299).build());
    }

    @Test
    void builderAllowsErrorWithNon2xxStatus() {
        Response<String> r300 = Response.<String>builder()
                .success(false).error("e").statusCode(300).build();
        assertEquals(300, r300.getStatusCode());

        Response<String> r199 = Response.<String>builder()
                .success(false).error("e").statusCode(199).build();
        assertEquals(199, r199.getStatusCode());
    }

    @Test
    void builderRejectsSuccessWithError() {
        assertThrows(IllegalStateException.class, () ->
                Response.<String>builder()
                        .success(true).error("should not be here").statusCode(200).build());
    }

    @Test
    void builderAllowsSuccessWithNullError() {
        Response<String> resp = Response.<String>builder()
                .success(true).statusCode(200).build();
        assertTrue(resp.isSuccess());
        assertNull(resp.getError());
    }
}
