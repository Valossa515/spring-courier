package io.github.valossa515.spring_courier.messaging;

import io.github.valossa515.spring_courier.outbox.OutboxMessage;

/**
 * Decides which topic an outbox message is published to.
 *
 * <p>Supply a bean of this type to route by anything the message carries — the
 * event type, a field inside the payload, a tenant prefix — instead of the
 * default naming.
 */
@FunctionalInterface
public interface OutboxTopicResolver {

    /**
     * Returns the topic for the given message.
     *
     * @param message message about to be published
     * @return topic name; never {@code null}
     */
    String resolve(OutboxMessage message);
}
