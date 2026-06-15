package io.github.valossa515.spring_courier.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcOutboxStoreTest {

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private JdbcOutboxStore store;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:outbox-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        new OutboxSchemaInitializer(dataSource, "courier_outbox").afterPropertiesSet();
        jdbc = new JdbcTemplate(dataSource);
        store = new JdbcOutboxStore(dataSource, "courier_outbox");
    }

    private OutboxMessage pending(String payload) {
        return new OutboxMessage(UUID.randomUUID().toString(), "com.example.Evt",
                payload, OutboxStatus.PENDING, 0, null, Instant.now(), null);
    }

    @Test
    void savesAndFetchesPendingOldestFirst() throws InterruptedException {
        OutboxMessage first = pending("{\"a\":1}");
        store.save(first);
        Thread.sleep(5);
        OutboxMessage second = pending("{\"a\":2}");
        store.save(second);

        List<OutboxMessage> pending = store.fetchPending(10);

        assertThat(pending).extracting(OutboxMessage::id)
                .containsExactly(first.id(), second.id());
        assertThat(pending.get(0).payload()).isEqualTo("{\"a\":1}");
        assertThat(pending.get(0).status()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void fetchPendingHonorsLimit() {
        store.save(pending("{}"));
        store.save(pending("{}"));
        store.save(pending("{}"));

        assertThat(store.fetchPending(2)).hasSize(2);
    }

    @Test
    void claimIsExclusive() {
        OutboxMessage m = pending("{}");
        store.save(m);

        assertThat(store.claim(m.id())).isTrue();
        assertThat(store.claim(m.id())).isFalse();
        assertThat(store.fetchPending(10)).isEmpty();
    }

    @Test
    void markProcessedRemovesFromPending() {
        OutboxMessage m = pending("{}");
        store.save(m);
        store.claim(m.id());

        store.markProcessed(m.id());

        assertThat(currentStatus(m.id())).isEqualTo("PROCESSED");
        assertThat(store.fetchPending(10)).isEmpty();
    }

    @Test
    void markFailedRetriesThenParksAsFailed() {
        OutboxMessage m = pending("{}");
        store.save(m);

        store.claim(m.id());
        store.markFailed(m.id(), "boom", 2);
        // attempt 1 < max 2 → back to PENDING
        assertThat(currentStatus(m.id())).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject(
                "SELECT attempts FROM courier_outbox WHERE id = ?", Integer.class, m.id()))
                .isEqualTo(1);

        store.claim(m.id());
        store.markFailed(m.id(), "boom again", 2);
        // attempt 2 >= max 2 → FAILED
        assertThat(currentStatus(m.id())).isEqualTo("FAILED");
        assertThat(store.fetchPending(10)).isEmpty();
    }

    @Test
    void reclaimStaleReturnsLongRunningProcessingToPending() {
        OutboxMessage m = pending("{}");
        store.save(m);
        store.claim(m.id());
        // force updated_at far into the past
        jdbc.update("UPDATE courier_outbox SET updated_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(3600)), m.id());

        int reclaimed = store.reclaimStale(Duration.ofMinutes(5));

        assertThat(reclaimed).isEqualTo(1);
        assertThat(currentStatus(m.id())).isEqualTo("PENDING");
    }

    @Test
    void reclaimStaleLeavesFreshProcessingAlone() {
        OutboxMessage m = pending("{}");
        store.save(m);
        store.claim(m.id());

        assertThat(store.reclaimStale(Duration.ofMinutes(5))).isZero();
        assertThat(currentStatus(m.id())).isEqualTo("PROCESSING");
    }

    @Test
    void rejectsIllegalTableName() {
        assertThatThrownBy(() -> new JdbcOutboxStore(dataSource, "outbox; DROP TABLE x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String currentStatus(String id) {
        return jdbc.queryForObject(
                "SELECT status FROM courier_outbox WHERE id = ?", String.class, id);
    }
}
