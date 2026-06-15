package io.github.valossa515.spring_courier.outbox;

/**
 * Lifecycle state of an {@link OutboxMessage}.
 *
 * <pre>
 *   PENDING ──claim──▶ PROCESSING ──success──▶ PROCESSED
 *      ▲                   │
 *      └──retry────────────┴──exhausted──▶ FAILED
 * </pre>
 */
public enum OutboxStatus {

    /** Persisted and awaiting dispatch. */
    PENDING,

    /** Claimed by a poller and currently being dispatched. */
    PROCESSING,

    /** Successfully dispatched to the handlers. */
    PROCESSED,

    /** Dispatch failed after exhausting the configured attempts. */
    FAILED
}
