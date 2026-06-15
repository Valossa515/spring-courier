package io.github.valossa515.spring_courier.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.valossa515.spring_courier.core.interfaces.INotification;

/**
 * Converts notifications to/from the JSON payload stored in the outbox.
 *
 * <p>The concrete notification type is stored alongside the payload (see
 * {@link OutboxMessage#eventType()}) and is the <em>only</em> type used during
 * deserialization, so no polymorphic type information is embedded in the JSON.
 */
public class OutboxSerializer {

    private final ObjectMapper objectMapper;

    public OutboxSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes a notification into its JSON payload.
     *
     * @throws OutboxException if the notification cannot be serialized
     */
    public String serialize(INotification event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new OutboxException(
                    "Failed to serialize notification " + event.getClass().getName(), e);
        }
    }

    /**
     * Reconstructs a notification from its stored type name and JSON payload.
     *
     * @throws OutboxException if the type is unknown or the payload is invalid
     */
    public INotification deserialize(String eventType, String payload) {
        try {
            Class<?> type = Class.forName(eventType);
            if (!INotification.class.isAssignableFrom(type)) {
                throw new OutboxException(
                        "Stored type is not an INotification: " + eventType);
            }
            return (INotification) objectMapper.readValue(payload, type);
        } catch (OutboxException e) {
            throw e;
        } catch (Exception e) {
            throw new OutboxException(
                    "Failed to deserialize outbox payload for type " + eventType, e);
        }
    }
}
