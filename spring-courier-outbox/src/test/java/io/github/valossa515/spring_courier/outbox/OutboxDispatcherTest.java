package io.github.valossa515.spring_courier.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.valossa515.spring_courier.core.Courier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Pins the {@link OutboxDispatcher} contract, which is what lets a module such
 * as {@code spring-courier-messaging} send the events to a broker instead of
 * republishing them in-process. The guarantees asserted here are the ones such
 * a dispatcher relies on, so they must not drift.
 */
class OutboxDispatcherTest {

    private JdbcTemplate jdbc;
    private JdbcOutboxStore store;
    private OutboxPublisher publisher;
    private OutboxSerializer serializer;
    private OutboxProperties properties;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:dispatcher-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        new OutboxSchemaInitializer(ds, "courier_outbox").afterPropertiesSet();
        jdbc = new JdbcTemplate(ds);
        serializer = new OutboxSerializer(new ObjectMapper());
        store = new JdbcOutboxStore(ds, "courier_outbox");
        publisher = new OutboxPublisher(store, serializer);
        properties = new OutboxProperties();
    }

    @Test
    void theDefaultDispatcherRepublishesInProcess() {
        Courier courier = mock(Courier.class);
        SampleNotification event = new SampleNotification("e-1", 7);
        publisher.publish(event);

        new OutboxPoller(store, new CourierOutboxDispatcher(serializer, courier), properties)
                .pollOnce();

        verify(courier).publish(event);
    }

    @Test
    void aCustomDispatcherReplacesInProcessDelivery() {
        Courier courier = mock(Courier.class);
        List<OutboxMessage> delivered = new ArrayList<>();
        publisher.publish(new SampleNotification("e-2", 8));

        new OutboxPoller(store, delivered::add, properties).pollOnce();

        assertThat(delivered)
                .as("supplying a dispatcher redirects delivery — it does not add to it")
                .hasSize(1);
        verifyNoInteractions(courier);
    }

    @Test
    void theDispatcherReceivesTheStoredPayloadUntouched() {
        List<OutboxMessage> delivered = new ArrayList<>();
        publisher.publish(new SampleNotification("e-3", 9));

        new OutboxPoller(store, delivered::add, properties).pollOnce();

        OutboxMessage message = delivered.get(0);
        assertThat(message.eventType())
                .isEqualTo(SampleNotification.class.getName());
        assertThat(message.payload())
                .as("a broker-bound dispatcher forwards this JSON as-is, with no "
                        + "deserialize/re-serialize round trip")
                .isEqualTo(jdbc.queryForObject(
                        "SELECT payload FROM courier_outbox", String.class));
    }

    @Test
    void aMessageIsMarkedProcessedOnlyAfterTheDispatcherReturns() {
        List<String> statusSeenDuringDispatch = new ArrayList<>();
        publisher.publish(new SampleNotification("e-4", 10));

        new OutboxPoller(store, message -> statusSeenDuringDispatch.add(currentStatus()),
                properties).pollOnce();

        assertThat(statusSeenDuringDispatch)
                .as("returning before delivery is durable would let a crash in this "
                        + "window lose the event — the very failure the outbox prevents")
                .containsExactly("PROCESSING");
        assertThat(currentStatus()).isEqualTo("PROCESSED");
    }

    @Test
    void aThrowingDispatcherLeavesTheMessagePendingForARetry() {
        properties.setMaxAttempts(5);
        publisher.publish(new SampleNotification("e-5", 11));

        new OutboxPoller(store, message -> {
            throw new IllegalStateException("broker unreachable");
        }, properties).pollOnce();

        assertThat(currentStatus())
                .as("throwing is the dispatcher's only way to say \"not delivered\"")
                .isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT last_error FROM courier_outbox", String.class))
                .contains("broker unreachable");
    }

    private String currentStatus() {
        return jdbc.queryForObject("SELECT status FROM courier_outbox", String.class);
    }
}
