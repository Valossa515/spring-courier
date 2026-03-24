package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.ResponseEntityConverter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DefaultResponseEntityConverterTest {

    private final ResponseEntityConverter converter = new DefaultResponseEntityConverter();

    @Test
    void convertSuccessResponseDelegatesToToEntity() {
        Response<String> resp = Response.success("data", 200);
        ResponseEntity<?> entity = converter.convert(resp);

        assertEquals(200, entity.getStatusCode().value());
        assertSame(resp, entity.getBody());
    }

    @Test
    void convertErrorResponsePreservesStatusAndBody() {
        Response<String> resp = Response.error("fail", 422);
        ResponseEntity<?> entity = converter.convert(resp);

        assertEquals(422, entity.getStatusCode().value());
        assertSame(resp, entity.getBody());
    }

    @Test
    void convertNullResponseReturnsInternalServerError() {
        ResponseEntity<?> entity = converter.convert(null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), entity.getStatusCode().value());
        assertNull(entity.getBody());
    }

    @Test
    void converterIsAssignableFromLambda() {
        ResponseEntityConverter custom = response -> ResponseEntity.ok("custom");

        ResponseEntity<?> entity = custom.convert(Response.success("ignored"));
        assertNotNull(entity);
        assertEquals("custom", entity.getBody());
    }
}
