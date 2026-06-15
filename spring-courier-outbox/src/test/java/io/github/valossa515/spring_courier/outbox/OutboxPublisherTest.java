package io.github.valossa515.spring_courier.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxStore store;

    @Captor
    private ArgumentCaptor<OutboxMessage> captor;

    private final OutboxSerializer serializer = new OutboxSerializer(new ObjectMapper());

    @Test
    void persistsPendingMessageWithSerializedPayload() {
        OutboxPublisher publisher = new OutboxPublisher(store, serializer);

        publisher.publish(new SampleNotification("o-1", 7));

        verify(store).save(captor.capture());
        OutboxMessage saved = captor.getValue();
        assertThat(saved.eventType()).isEqualTo(SampleNotification.class.getName());
        assertThat(saved.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.attempts()).isZero();
        assertThat(saved.id()).isNotBlank();
        assertThat(saved.payload()).contains("\"id\":\"o-1\"").contains("\"value\":7");
    }

    @Test
    void rejectsNullNotification() {
        OutboxPublisher publisher = new OutboxPublisher(store, serializer);

        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
