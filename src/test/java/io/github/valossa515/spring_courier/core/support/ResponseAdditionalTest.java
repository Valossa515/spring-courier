package io.github.valossa515.spring_courier.core.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseAdditionalTest {
    @Test
    void errorFromThrowableWithStatusCode() {
        Throwable t = new IllegalStateException("t-problem");
        Response<String> resp = Response.error(t, 503);
        assertFalse(resp.isSuccess());
        assertEquals("t-problem", resp.getError());
        assertEquals(503, resp.getStatusCode());
    }

    @Test
    void getDataOrThrowWithProvidedResponseException() {
        Response.ResponseException custom = new Response.ResponseException("custom", 499);
        Response<String> resp = Response.error("err", 400);
        Response.ResponseException thrown = assertThrows(Response.ResponseException.class,
                () -> resp.getDataOrThrow(custom));
        assertSame(custom, thrown);
    }
}