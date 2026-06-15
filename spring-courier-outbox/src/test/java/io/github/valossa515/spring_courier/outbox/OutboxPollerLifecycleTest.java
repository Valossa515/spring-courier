package io.github.valossa515.spring_courier.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.valossa515.spring_courier.core.Courier;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OutboxPollerLifecycleTest {

    private OutboxPoller poller;

    @AfterEach
    void tearDown() {
        if (poller != null) {
            poller.stop();
        }
    }

    @Test
    void startsPollsAndStops() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:life-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        new OutboxSchemaInitializer(ds, "courier_outbox").afterPropertiesSet();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        OutboxSerializer serializer = new OutboxSerializer(new ObjectMapper());
        OutboxStore store = new JdbcOutboxStore(ds, "courier_outbox");
        Courier courier = mock(Courier.class);
        OutboxProperties props = new OutboxProperties();
        props.setPollDelayMs(50);
        poller = new OutboxPoller(store, serializer, courier, props);

        new OutboxPublisher(store, serializer).publish(new SampleNotification("l-1", 5));

        poller.start();
        poller.start(); // idempotent: second call is a no-op
        assertThat(poller.isRunning()).isTrue();

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM courier_outbox WHERE status = 'PROCESSED'",
                        Integer.class)).isEqualTo(1));

        poller.stop();
        assertThat(poller.isRunning()).isFalse();
    }

    @Test
    void keepsRunningWhenAPollCycleThrows() throws InterruptedException {
        OutboxStore failing = mock(OutboxStore.class);
        when(failing.reclaimStale(any())).thenThrow(new RuntimeException("db down"));
        OutboxProperties props = new OutboxProperties();
        props.setPollDelayMs(20);
        poller = new OutboxPoller(failing, new OutboxSerializer(new ObjectMapper()),
                mock(Courier.class), props);

        poller.start();
        Thread.sleep(120); // let several failing cycles run

        assertThat(poller.isRunning()).isTrue();
    }
}
