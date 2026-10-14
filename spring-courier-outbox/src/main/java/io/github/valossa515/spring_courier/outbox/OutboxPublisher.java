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
     * @throws OutboxException if the event type is generic, or cannot be
     *                         serialized or persisted
     */
    public void publish(INotification event) {
        if (event == null) {
            throw new IllegalArgumentException("notification must not be null");
        }
        rejectGenericType(event.getClass());
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

    /**
     * Rejects notification types that carry type parameters, because the outbox
     * cannot round-trip them.
     *
     * <p>Only the raw class name is stored (see {@link OutboxMessage#eventType()}),
     * and type erasure means the arguments are not recoverable from it. On the
     * way back, {@code Changed<Product>} is read as a raw {@code Changed} and its
     * component arrives as a {@code LinkedHashMap}, so the handler fails with a
     * {@link ClassCastException}.
     *
     * <p>Failing here is the point. That {@code ClassCastException} would
     * otherwise surface inside the background poller — counted as a delivery
     * attempt, retried, and eventually parked as {@code FAILED} long after the
     * transaction that caused it has committed. Throwing during {@code publish}
     * keeps the failure in the caller's transaction, where the stack trace names
     * the culprit and nothing is persisted.
     *
     * <p>This is a limitation of in-process delivery, not of the outbox table: a
     * broker-bound {@link OutboxDispatcher} forwards the stored JSON untouched
     * and never deserializes it. The check still applies, because the dispatcher
     * in use is a deployment choice the publisher cannot see.
     */
    private static void rejectGenericType(Class<?> eventType) {
        if (eventType.getTypeParameters().length == 0) {
            return;
        }
        throw new OutboxException("Cannot publish generic notification type "
                + eventType.getName() + ": type arguments are erased, so the "
                + "payload cannot be deserialized back into it. Use a concrete "
                + "notification type instead (e.g. ProductChangedNotification "
                + "rather than Changed<Product>).");
    }
}
