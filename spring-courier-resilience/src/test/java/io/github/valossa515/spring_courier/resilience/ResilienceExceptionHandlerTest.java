package io.github.valossa515.spring_courier.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;

class ResilienceExceptionHandlerTest {

    private final ResilienceExceptionHandler handler = new ResilienceExceptionHandler();
    private final SampleRequest request = new SampleRequest("r-1");

    @Test
    void mapsAnOpenCircuitTo503() {
        CircuitBreaker breaker = CircuitBreaker.ofDefaults("test");
        breaker.transitionToOpenState();

        Response<?> response = handler.handle(request,
                CallNotPermittedException.createCallNotPermittedException(breaker));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(503);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).contains("circuit is open");
    }

    @Test
    void mapsAnExhaustedRateLimitTo429() {
        Response<?> response = handler.handle(request,
                RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("test")));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(429);
        assertThat(response.getError()).contains("rate limit");
    }

    @Test
    void mapsASaturatedBulkheadTo503() {
        Response<?> response = handler.handle(request,
                BulkheadFullException.createBulkheadFullException(Bulkhead.ofDefaults("test")));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(503);
        assertThat(response.getError()).contains("concurrent");
    }

    @Test
    void passesUnrelatedExceptionsOn() {
        assertThat(handler.handle(request, new IllegalStateException("something else")))
                .as("returning null lets the next handler, or core, deal with it")
                .isNull();
    }
}
