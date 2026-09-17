package io.github.valossa515.spring_courier.outbox;

/**
 * Where a drained outbox message is delivered.
 *
 * <p>The default implementation ({@link CourierOutboxDispatcher}) deserializes
 * the message and republishes it in-process through {@code Courier}. Swap in a
 * different implementation (e.g. the {@code spring-courier-messaging} module)
 * to hand the message to a broker instead, which is what makes the events
 * visible to other services.
 *
 * <p>Implementations receive the stored {@link OutboxMessage} rather than a
 * deserialized event on purpose: a broker-bound dispatcher can forward the
 * JSON payload as-is instead of paying a deserialize/re-serialize round trip,
 * and it keeps the choice of representation with whoever does the delivering.
 *
 * <p>A dispatcher signals failure by throwing: the poller then records the
 * attempt and leaves the message for a later retry, so delivery stays
 * at-least-once.
 */
@FunctionalInterface
public interface OutboxDispatcher {

    /**
     * Delivers a single outbox message.
     *
     * @param message the message drained from the outbox
     * @throws RuntimeException when delivery fails; the poller records the
     *         failed attempt and retries later
     */
    void dispatch(OutboxMessage message);
}
