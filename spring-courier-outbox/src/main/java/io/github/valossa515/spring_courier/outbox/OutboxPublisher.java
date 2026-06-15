package io.github.valossa515.spring_courier.outbox;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the transactional outbox.
 *
 * <p>Call {@link #publish(INotification)} from inside a transactional command
 * handler: the event is persisted in the <em>same</em> transaction as the
 * business data, so it is stored if and only if the transaction commits. A
 * background {@link OutboxPoller} then delivers it to the notification handlers
 * (at-least-once), decoupling event delivery from the request thread.
 *
 * <pre>{@code
 * @Transactional
 * public Order handle(CreateOrderCommand cmd) {
 *     Order order = repository.save(new Order(cmd));
 *     outboxPublisher.publish(new OrderCreatedNotification(order.id()));
 *     return order;
 * }
 * }</pre>
 */
public class OutboxPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxStore store;
    private final OutboxSerializer serializer;

    public OutboxPublisher(OutboxStore store, OutboxSerializer serializer) {
        this.store = store;
        this.serializer = serializer;
    }

    /**
     * Captures a notification into the outbox within the current transaction.
     *
     * @param event the notification to deliver later; must be non-null
     * @throws OutboxException if the event cannot be serialized or persisted
     */
    public void publish(INotification event) {
        if (event == null) {
            throw new IllegalArgumentException("notification must not be null");
        }
        OutboxMessage message = new OutboxMessage(
                UUID.randomUUID().toString(),
                event.getClass().getName(),
                serializer.serialize(event),
                OutboxStatus.PENDING,
                0,
                null,
                Instant.now(),
                null);
        store.save(message);
        LOG.debug("Captured notification {} into outbox as {}",
                message.eventType(), message.id());
    }
}
