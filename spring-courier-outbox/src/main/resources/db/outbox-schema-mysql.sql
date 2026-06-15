-- Spring Courier — transactional outbox schema (MySQL 8+)
-- Apply via Flyway/Liquibase or manually. Adjust the table name if you set
-- spring.courier.outbox.table-name to something other than courier_outbox.

CREATE TABLE IF NOT EXISTS courier_outbox (
    id            VARCHAR(36)    NOT NULL,
    event_type    VARCHAR(512)   NOT NULL,
    payload       LONGTEXT       NOT NULL,
    status        VARCHAR(20)    NOT NULL,
    attempts      INT            NOT NULL DEFAULT 0,
    last_error    VARCHAR(2048),
    created_at    TIMESTAMP      NOT NULL,
    updated_at    TIMESTAMP      NOT NULL,
    processed_at  TIMESTAMP      NULL,
    PRIMARY KEY (id),
    INDEX idx_courier_outbox_status (status, created_at)
);
