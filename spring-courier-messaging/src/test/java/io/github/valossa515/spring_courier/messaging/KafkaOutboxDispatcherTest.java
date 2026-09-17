package io.github.valossa515.spring_courier.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.valossa515.spring_courier.outbox.OutboxMessage;
import io.github.valossa515.spring_courier.outbox.OutboxStatus;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class KafkaOutboxDispatcherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> template = mock(KafkaTemplate.class);

    private KafkaOutboxDispatcher dispatcher;

    private final OutboxMessage message = new OutboxMessage(
            "id-1", "com.example.OrderCreatedNotification", "{\"orderId\":\"o-1\"}",
            OutboxStatus.PENDING, 0, null, Instant.now(), null);

    @BeforeEach
    void setUp() {
        dispatcher = new KafkaOutboxDispatcher(template,
                new EventTypeTopicResolver("courier.", null), Duration.ofSeconds(5));
    }

    @SuppressWarnings("unchecked")
    private void templateAccepts() {
        when(template.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @SuppressWarnings("unchecked")
    private ProducerRecord<String, String> capturedRecord() {
        ArgumentCaptor<ProducerRecord<String, String>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void sendsThePayloadAsIsToTheResolvedTopic() {
        templateAccepts();

        dispatcher.dispatch(message);

        ProducerRecord<String, String> record = capturedRecord();
        assertThat(record.topic()).isEqualTo("courier.OrderCreatedNotification");
        assertThat(record.key()).isEqualTo("id-1");
        assertThat(record.value())
                .as("the stored JSON is forwarded without a deserialize/serialize round trip")
                .isEqualTo("{\"orderId\":\"o-1\"}");
    }

    @Test
    void carriesTheOutboxIdAndEventTypeAsHeaders() {
        templateAccepts();

        dispatcher.dispatch(message);

        ProducerRecord<String, String> record = capturedRecord();
        assertThat(header(record, KafkaOutboxDispatcher.HEADER_MESSAGE_ID))
                .as("delivery is at-least-once, so consumers need a stable id to deduplicate on")
                .isEqualTo("id-1");
        assertThat(header(record, KafkaOutboxDispatcher.HEADER_EVENT_TYPE))
                .isEqualTo("com.example.OrderCreatedNotification");
    }

    @Test
    void failsWhenTheBrokerRejectsTheSend() {
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker down"));
        when(template.send(any(ProducerRecord.class))).thenReturn(failed);

        assertThatThrownBy(() -> dispatcher.dispatch(message))
                .as("throwing is what makes the poller leave the message pending for a retry")
                .isInstanceOf(OutboxPublishException.class)
                .hasMessageContaining("id-1");
    }

    @Test
    void failsWhenTheAcknowledgementDoesNotArriveInTime() {
        // A future that never completes: the send was queued but never acked.
        when(template.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<>());
        var impatient = new KafkaOutboxDispatcher(template,
                new EventTypeTopicResolver("courier.", null), Duration.ofMillis(50));

        assertThatThrownBy(() -> impatient.dispatch(message))
                .as("returning before the ack would let the poller mark a message delivered "
                        + "that Kafka had not accepted")
                .isInstanceOf(OutboxPublishException.class);
    }

    @Test
    void honorsAFixedTopicWhenConfigured() {
        templateAccepts();
        var fixed = new KafkaOutboxDispatcher(template,
                new EventTypeTopicResolver("ignored.", "domain-events"), Duration.ofSeconds(5));

        fixed.dispatch(message);

        assertThat(capturedRecord().topic()).isEqualTo("domain-events");
    }

    private static String header(ProducerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
