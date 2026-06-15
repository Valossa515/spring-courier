package io.github.valossa515.spring_courier.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OutboxPollerTest {

    private JdbcTemplate jdbc;
    private JdbcOutboxStore store;
    private OutboxPublisher publisher;
    private Courier courier;
    private OutboxProperties properties;
    private OutboxPoller poller;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:poller-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        new OutboxSchemaInitializer(ds, "courier_outbox").afterPropertiesSet();
        jdbc = new JdbcTemplate(ds);
        OutboxSerializer serializer = new OutboxSerializer(new ObjectMapper());
        store = new JdbcOutboxStore(ds, "courier_outbox");
        publisher = new OutboxPublisher(store, serializer);
        courier = mock(Courier.class);
        properties = new OutboxProperties();
        poller = new OutboxPoller(store, serializer, courier, properties);
    }

    @Test
    void dispatchesPendingMessagesAndMarksProcessed() {
        SampleNotification event = new SampleNotification("e-1", 99);
        publisher.publish(event);

        poller.pollOnce();

        verify(courier).publish(event);
        assertThat(status("PROCESSED")).isEqualTo(1);
        assertThat(store.fetchPending(10)).isEmpty();
    }

    @Test
    void recordsFailureAndRetriesUntilMaxAttempts() {
        properties.setMaxAttempts(2);
        doThrow(new RuntimeException("handler boom")).when(courier).publish(any(INotification.class));
        publisher.publish(new SampleNotification("e-2", 1));

        poller.pollOnce(); // attempt 1 → back to PENDING
        assertThat(status("PENDING")).isEqualTo(1);

        poller.pollOnce(); // attempt 2 → FAILED
        assertThat(status("FAILED")).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT last_error FROM courier_outbox", String.class))
                .contains("handler boom");
    }

    @Test
    void doesNothingWhenOutboxEmpty() {
        poller.pollOnce();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courier_outbox", Integer.class))
                .isZero();
    }

    private int status(String status) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM courier_outbox WHERE status = ?", Integer.class, status);
    }
}
