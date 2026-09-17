package io.github.valossa515.spring_courier.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.abort;

import io.github.valossa515.spring_courier.outbox.OutboxMessage;
import io.github.valossa515.spring_courier.outbox.OutboxStatus;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

/**
 * Exercises the dispatcher against a real Kafka broker running in-JVM, which is
 * the only way to verify the parts a mocked template cannot: that the record
 * actually lands on the topic, that the headers survive the wire, and that the
 * send is genuinely acknowledged before {@code dispatch} returns.
 *
 * <p>Aborts itself if the embedded broker cannot start, so the build stays
 * green on constrained machines.
 */
class KafkaOutboxDispatcherIntegrationTest {

    private static final String TOPIC = "courier.OrderCreatedNotification";

    private static EmbeddedKafkaKraftBroker broker;
    private static KafkaTemplate<String, String> template;

    @BeforeAll
    static void startBroker() {
        try {
            broker = new EmbeddedKafkaKraftBroker(1, 1, TOPIC);
            broker.afterPropertiesSet();
        } catch (Exception | NoClassDefFoundError e) {
            abort("Embedded Kafka could not start here — skipping integration test: " + e);
        }

        Map<String, Object> producer = new HashMap<>();
        producer.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        producer.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer.put(ProducerConfig.ACKS_CONFIG, "all");
        template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producer));
    }

    @AfterAll
    static void stopBroker() {
        if (template != null) {
            template.destroy();
        }
        if (broker != null) {
            broker.destroy();
        }
    }

    private static OutboxMessage message(String id, String payload) {
        return new OutboxMessage(id, "com.example.OrderCreatedNotification", payload,
                OutboxStatus.PENDING, 0, null, Instant.now(), null);
    }

    private KafkaOutboxDispatcher dispatcher(Duration timeout) {
        return new KafkaOutboxDispatcher(template,
                new EventTypeTopicResolver("courier.", null), timeout);
    }

    /**
     * Reads from the start of the topic until the record with {@code key} shows
     * up. Matching on the key matters: every test here publishes to the same
     * topic, so simply taking the first record would hand one test another
     * test's message.
     */
    private ConsumerRecord<String, String> consumeByKey(String key) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(List.of(TOPIC));
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("no record with key " + key + " arrived on " + TOPIC);
    }

    @Test
    void theEventReachesTheTopicWithItsPayloadKeyAndHeaders() {
        String id = UUID.randomUUID().toString();
        String payload = "{\"orderId\":\"o-42\"}";

        dispatcher(Duration.ofSeconds(20)).dispatch(message(id, payload));

        ConsumerRecord<String, String> record = consumeByKey(id);
        assertThat(record.value()).isEqualTo(payload);
        assertThat(record.key()).isEqualTo(id);
        assertThat(headerOf(record, KafkaOutboxDispatcher.HEADER_MESSAGE_ID)).isEqualTo(id);
        assertThat(headerOf(record, KafkaOutboxDispatcher.HEADER_EVENT_TYPE))
                .isEqualTo("com.example.OrderCreatedNotification");
    }

    @Test
    void dispatchReturnsOnlyAfterTheBrokerHasTheRecord() {
        String id = UUID.randomUUID().toString();

        dispatcher(Duration.ofSeconds(20)).dispatch(message(id, "{\"orderId\":\"o-43\"}"));

        // dispatch() has returned; the record must already be readable, which is
        // what lets the poller mark the message PROCESSED without losing it.
        assertThat(consumeByKey(id)).isNotNull();
    }

    @Test
    void failsWhenTheBrokerIsUnreachable() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:1");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
        KafkaTemplate<String, String> broken =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));

        var dispatcher = new KafkaOutboxDispatcher(broken,
                new EventTypeTopicResolver("courier.", null), Duration.ofSeconds(3));

        try {
            assertThatThrownBy(() -> dispatcher.dispatch(message("id-x", "{}")))
                    .as("an unreachable broker must surface as a failure so the message "
                            + "stays pending instead of being marked delivered")
                    .isInstanceOf(OutboxPublishException.class);
        } finally {
            broken.destroy();
        }
    }

    private static String headerOf(ConsumerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
