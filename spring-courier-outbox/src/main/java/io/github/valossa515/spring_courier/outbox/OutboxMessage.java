package io.github.valossa515.spring_courier.outbox;

import java.time.Instant;

/**
 * A single row of the transactional outbox: a notification captured inside a
 * business transaction, to be dispatched later by the {@link OutboxPoller}.
 *
 * @param id          unique identifier (UUID string)
 * @param eventType   fully-qualified class name of the original notification
 * @param payload     JSON-serialized notification body
 * @param status      current {@link OutboxStatus}
 * @param attempts    number of dispatch attempts made so far
 * @param lastError   last failure message, or {@code null}
 * @param createdAt   instant the message was persisted
 * @param processedAt instant the message reached a terminal state, or {@code null}
 */
public record OutboxMessage(
        String id,
        String eventType,
        String payload,
        OutboxStatus status,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant processedAt) {
}
