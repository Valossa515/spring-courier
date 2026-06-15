package io.github.valossa515.spring_courier.outbox;

import java.time.Duration;
import java.util.List;

/**
 * Persistence boundary for the transactional outbox.
 *
 * <p>{@link #save(OutboxMessage)} is expected to participate in the caller's
 * ambient transaction (so the message commits atomically with the business
 * data); the poller-facing methods manage their own short transactions.
 */
public interface OutboxStore {

    /**
     * Persists a message. Must join the current transaction, if any.
     */
    void save(OutboxMessage message);

    /**
     * Returns up to {@code limit} messages eligible for dispatch
     * (oldest first).
     */
    List<OutboxMessage> fetchPending(int limit);

    /**
     * Atomically transitions a message from {@code PENDING} to
     * {@code PROCESSING}.
     *
     * @return {@code true} if this caller won the claim; {@code false} if the
     *         message was already claimed or is no longer pending
     */
    boolean claim(String id);

    /**
     * Marks a successfully dispatched message as {@code PROCESSED}.
     */
    void markProcessed(String id);

    /**
     * Records a failed dispatch attempt. The message returns to
     * {@code PENDING} for a future retry, or is marked {@code FAILED} once
     * {@code maxAttempts} is reached.
     */
    void markFailed(String id, String error, int maxAttempts);

    /**
     * Returns messages stuck in {@code PROCESSING} for longer than
     * {@code timeout} back to {@code PENDING} (crash recovery).
     *
     * @return number of messages reclaimed
     */
    int reclaimStale(Duration timeout);
}
