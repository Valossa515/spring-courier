package io.github.valossa515.spring_courier.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OutboxPropertiesTest {

    @Test
    void hasSensibleDefaults() {
        OutboxProperties p = new OutboxProperties();

        assertThat(p.isEnabled()).isFalse();
        assertThat(p.getPollDelayMs()).isEqualTo(5_000);
        assertThat(p.getBatchSize()).isEqualTo(100);
        assertThat(p.getMaxAttempts()).isEqualTo(5);
        assertThat(p.getTableName()).isEqualTo("courier_outbox");
        assertThat(p.isAutoCreateSchema()).isFalse();
        assertThat(p.getProcessingTimeoutMs()).isEqualTo(60_000);
    }

    @Test
    void honorsSetters() {
        OutboxProperties p = new OutboxProperties();

        p.setEnabled(true);
        p.setPollDelayMs(250);
        p.setBatchSize(10);
        p.setMaxAttempts(3);
        p.setTableName("events");
        p.setAutoCreateSchema(true);
        p.setProcessingTimeoutMs(1_000);

        assertThat(p.isEnabled()).isTrue();
        assertThat(p.getPollDelayMs()).isEqualTo(250);
        assertThat(p.getBatchSize()).isEqualTo(10);
        assertThat(p.getMaxAttempts()).isEqualTo(3);
        assertThat(p.getTableName()).isEqualTo("events");
        assertThat(p.isAutoCreateSchema()).isTrue();
        assertThat(p.getProcessingTimeoutMs()).isEqualTo(1_000);
    }
}
