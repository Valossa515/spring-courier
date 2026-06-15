package io.github.valossa515.spring_courier.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OutboxSerializerTest {

    private final OutboxSerializer serializer = new OutboxSerializer(new ObjectMapper());

    @Test
    void roundTripsNotification() {
        SampleNotification event = new SampleNotification("abc", 42);

        String payload = serializer.serialize(event);
        Object restored = serializer.deserialize(SampleNotification.class.getName(), payload);

        assertThat(restored).isEqualTo(event);
    }

    @Test
    void failsOnUnknownType() {
        assertThatThrownBy(() ->
                serializer.deserialize("com.example.DoesNotExist", "{}"))
                .isInstanceOf(OutboxException.class);
    }

    @Test
    void failsWhenTypeIsNotANotification() {
        assertThatThrownBy(() ->
                serializer.deserialize(String.class.getName(), "\"x\""))
                .isInstanceOf(OutboxException.class)
                .hasMessageContaining("not an INotification");
    }

    @Test
    void failsOnInvalidPayload() {
        assertThatThrownBy(() ->
                serializer.deserialize(SampleNotification.class.getName(), "not-json"))
                .isInstanceOf(OutboxException.class);
    }
}
