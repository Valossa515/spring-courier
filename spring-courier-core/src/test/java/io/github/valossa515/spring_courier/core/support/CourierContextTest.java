package io.github.valossa515.spring_courier.core.support;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierContextTest {

    @Test
    void createGeneratesCorrelationId() {
        CourierContext ctx = CourierContext.create();
        assertNotNull(ctx.getCorrelationId());
        assertFalse(ctx.getCorrelationId().isBlank());
        assertTrue(ctx.getMetadata().isEmpty());
    }

    @Test
    void createWithCorrelationId() {
        CourierContext ctx = CourierContext.create("abc-123");
        assertEquals("abc-123", ctx.getCorrelationId());
    }

    @Test
    void createWithNullCorrelationIdGeneratesOne() {
        CourierContext ctx = CourierContext.create(null);
        assertNotNull(ctx.getCorrelationId());
    }

    @Test
    void createWithCorrelationIdAndMetadata() {
        Map<String, Object> meta =
                Map.of("tenant", "acme", "userId", 42);
        CourierContext ctx = CourierContext.create("xyz", meta);
        assertEquals("xyz", ctx.getCorrelationId());
        assertEquals(2, ctx.getMetadata().size());
    }

    @Test
    void createWithNullMetadataDefaultsToEmpty() {
        CourierContext ctx = CourierContext.create("id", null);
        assertTrue(ctx.getMetadata().isEmpty());
    }

    @Test
    void withReturnsNewContextWithAddedEntry() {
        CourierContext original = CourierContext.create("id-1");
        CourierContext updated = original.with("key", "value");

        assertTrue(original.getMetadata().isEmpty());
        assertEquals("value",
                updated.<String>get("key").orElse(null));
        assertEquals("id-1", updated.getCorrelationId());
    }

    @Test
    void getReturnsEmptyForMissingKey() {
        CourierContext ctx = CourierContext.create();
        Optional<String> result = ctx.get("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void toStringContainsCorrelationId() {
        CourierContext ctx = CourierContext.create("test-id");
        String str = ctx.toString();
        assertTrue(str.contains("test-id"));
        assertTrue(str.contains("CourierContext"));
    }
}
