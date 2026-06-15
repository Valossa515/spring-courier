package io.github.valossa515.spring_courier.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the transactional outbox, under the
 * {@code spring.courier.outbox} prefix.
 *
 * <pre>
 *   spring.courier.outbox.enabled=true
 *   spring.courier.outbox.poll-delay-ms=5000
 *   spring.courier.outbox.batch-size=100
 *   spring.courier.outbox.max-attempts=5
 *   spring.courier.outbox.table-name=courier_outbox
 *   spring.courier.outbox.auto-create-schema=false
 *   spring.courier.outbox.processing-timeout-ms=60000
 * </pre>
 */
@ConfigurationProperties(prefix = "spring.courier.outbox")
public class OutboxProperties {

    /** Master switch for the outbox module. Disabled by default. */
    private boolean enabled = false;

    /** Delay between poll cycles, in milliseconds. */
    private long pollDelayMs = 5_000;

    /** Maximum number of messages dispatched per poll cycle. */
    private int batchSize = 100;

    /** Dispatch attempts before a message is parked as {@code FAILED}. */
    private int maxAttempts = 5;

    /** Outbox table name (alphanumeric/underscore only). */
    private String tableName = "courier_outbox";

    /**
     * When {@code true}, creates the outbox table at startup if missing.
     * Intended for development; production should manage the schema with the
     * provided DDL (Flyway/Liquibase).
     */
    private boolean autoCreateSchema = false;

    /**
     * How long a message may stay {@code PROCESSING} before a poller assumes
     * the previous worker crashed and returns it to {@code PENDING}.
     */
    private long processingTimeoutMs = 60_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollDelayMs() {
        return pollDelayMs;
    }

    public void setPollDelayMs(long pollDelayMs) {
        this.pollDelayMs = pollDelayMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public boolean isAutoCreateSchema() {
        return autoCreateSchema;
    }

    public void setAutoCreateSchema(boolean autoCreateSchema) {
        this.autoCreateSchema = autoCreateSchema;
    }

    public long getProcessingTimeoutMs() {
        return processingTimeoutMs;
    }

    public void setProcessingTimeoutMs(long processingTimeoutMs) {
        this.processingTimeoutMs = processingTimeoutMs;
    }
}
