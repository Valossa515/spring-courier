package io.github.valossa515.spring_courier.outbox;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.INotification;

/**
 * Default {@link OutboxDispatcher}: deserializes the stored message and
 * republishes it in-process through {@link Courier}, so the application's own
 * notification handlers receive it.
 *
 * <p>This keeps the outbox's original behaviour — the events never leave the
 * JVM. Use a broker-bound dispatcher when other services need to see them.
 */
public class CourierOutboxDispatcher implements OutboxDispatcher {

    private final OutboxSerializer serializer;
    private final Courier courier;

    /**
     * Creates the dispatcher.
     *
     * @param serializer turns the stored payload back into a notification
     * @param courier    dispatcher the notification is published through
     */
    public CourierOutboxDispatcher(OutboxSerializer serializer, Courier courier) {
        this.serializer = serializer;
        this.courier = courier;
    }

    @Override
    public void dispatch(OutboxMessage message) {
        INotification event = serializer.deserialize(message.eventType(), message.payload());
        courier.publish(event);
    }
}
