package io.github.valossa515.spring_courier.core.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable message metadata container that flows through the request
 * pipeline. Carries a correlation ID for distributed tracing and
 * arbitrary key-value metadata for cross-cutting concerns.
 *
 * <p>Bound to the current thread via {@link CourierContextHolder}.
 *
 * <p>Example usage in a pipeline behavior:
 * <pre>{@code
 * CourierContext ctx = CourierContextHolder.getContext();
 * String correlationId = ctx.getCorrelationId();
 * MDC.put("correlationId", correlationId);
 * }</pre>
 */
public final class CourierContext {

    private final String correlationId;
    private final Map<String, Object> metadata;

    private CourierContext(String correlationId,
            Map<String, Object> metadata) {
        this.correlationId = correlationId;
        this.metadata = Collections.unmodifiableMap(
                new HashMap<>(metadata));
    }

    /**
     * Creates a new context with an auto-generated correlation ID.
     */
    public static CourierContext create() {
        return new CourierContext(UUID.randomUUID().toString(),
                Collections.emptyMap());
    }

    /**
     * Creates a new context with the specified correlation ID.
     */
    public static CourierContext create(String correlationId) {
        return new CourierContext(
                correlationId != null ? correlationId
                        : UUID.randomUUID().toString(),
                Collections.emptyMap());
    }

    /**
     * Creates a new context with a correlation ID and metadata.
     */
    public static CourierContext create(String correlationId,
            Map<String, Object> metadata) {
        return new CourierContext(
                correlationId != null ? correlationId
                        : UUID.randomUUID().toString(),
                metadata != null ? metadata
                        : Collections.emptyMap());
    }

    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Returns an unmodifiable view of the metadata map.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Returns a metadata value by key.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) metadata.get(key));
    }

    /**
     * Returns a new context with an additional metadata entry.
     */
    public CourierContext with(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put(key, value);
        return new CourierContext(correlationId, newMetadata);
    }

    @Override
    public String toString() {
        return "CourierContext{correlationId='" + correlationId
                + "', metadata=" + metadata + '}';
    }
}
