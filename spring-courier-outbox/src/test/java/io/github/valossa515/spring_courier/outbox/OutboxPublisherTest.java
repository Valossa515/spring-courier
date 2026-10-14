package io.github.valossa515.spring_courier.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import java.util.Map;
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

    /** A generic notification: only the raw class name would be stored. */
    record Changed<T>(T value) implements INotification { }

    @Test
    void rejectsGenericNotificationTypes() {
        OutboxPublisher publisher = new OutboxPublisher(store, serializer);

        assertThatThrownBy(() -> publisher.publish(new Changed<>(new SampleNotification("o-1", 7))))
                .isInstanceOf(OutboxException.class)
                .hasMessageContaining("Changed")
                .hasMessageContaining("concrete notification type");
    }

    @Test
    void aRejectedNotificationIsNeverPersisted() {
        OutboxPublisher publisher = new OutboxPublisher(store, serializer);

        assertThatThrownBy(() -> publisher.publish(new Changed<>("x")))
                .isInstanceOf(OutboxException.class);

        // Throwing before the insert is what keeps the caller's transaction
        // free of a message the poller could never deliver.
        verifyNoInteractions(store);
    }

    /**
     * Pins the failure this check exists to prevent: stored and read back, the
     * component returns as a {@code LinkedHashMap} and the handler blows up —
     * asynchronously, inside the poller, long after the transaction committed.
     */
    @Test
    void theRoundTripThisPrevents() {
        Changed<SampleNotification> event = new Changed<>(new SampleNotification("o-1", 7));
        String payload = serializer.serialize(event);

        INotification back = serializer.deserialize(event.getClass().getName(), payload);

        assertThat(((Changed<?>) back).value())
                .as("type arguments are erased, so Jackson has no target type for the component")
                .isInstanceOf(Map.class)
                .isNotInstanceOf(SampleNotification.class);
    }
}
