package io.github.valossa515.spring_courier.core.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * A {@link Response} must survive a JSON round trip: distributed stores (such
 * as the Redis cache module) serialize whatever a handler returns, and handlers
 * are allowed to return a {@code Response} directly.
 */
class ResponseJsonRoundTripTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void successfulResponseSurvivesRoundTrip() throws Exception {
        Response<String> original = Response.success("payload");

        String json = mapper.writeValueAsString(original);
        Response<String> restored =
                mapper.readValue(json, new TypeReference<Response<String>>() { });

        assertThat(restored.getData()).isEqualTo("payload");
        assertThat(restored.isSuccess()).isTrue();
        assertThat(restored.getStatusCode()).isEqualTo(200);
        assertThat(restored.getError()).isNull();
    }

    @Test
    void customStatusCodeSurvivesRoundTrip() throws Exception {
        Response<String> original = Response.success("created", 201);

        Response<String> restored = mapper.readValue(
                mapper.writeValueAsString(original),
                new TypeReference<Response<String>>() { });

        assertThat(restored.getStatusCode()).isEqualTo(201);
        assertThat(restored.getData()).isEqualTo("created");
    }

    @Test
    void errorResponseSurvivesRoundTrip() throws Exception {
        Response<String> original = Response.error("boom", 500);

        Response<String> restored = mapper.readValue(
                mapper.writeValueAsString(original),
                new TypeReference<Response<String>>() { });

        assertThat(restored.isSuccess()).isFalse();
        assertThat(restored.getError()).isEqualTo("boom");
        assertThat(restored.getStatusCode()).isEqualTo(500);
    }
}
