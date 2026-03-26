package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryBehaviorTest {

    record TestCmd() implements ICommand<String> {
    }

    @Test
    @SuppressWarnings("unchecked")
    void successOnFirstAttemptDoesNotRetry() {
        RetryBehavior<IRequest<Response<String>>, Response<String>>
                behavior = new RetryBehavior<>(3, 10, 2.0);
        AtomicInteger counter = new AtomicInteger(0);

        Response<String> result = behavior.handle(
                (IRequest<Response<String>>) (IRequest<?>) new TestCmd(),
                () -> {
                    counter.incrementAndGet();
                    return Response.success("ok");
                });

        assertTrue(result.isSuccess());
        assertEquals(1, counter.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void retriesOnFailureAndSucceeds() {
        RetryBehavior<IRequest<Response<String>>, Response<String>>
                behavior = new RetryBehavior<>(3, 10, 1.0);
        AtomicInteger counter = new AtomicInteger(0);

        Response<String> result = behavior.handle(
                (IRequest<Response<String>>) (IRequest<?>) new TestCmd(),
                () -> {
                    int attempt = counter.incrementAndGet();
                    if (attempt < 3) {
                        throw new RuntimeException(
                                "fail attempt " + attempt);
                    }
                    return Response.success("recovered");
                });

        assertTrue(result.isSuccess());
        assertEquals("recovered", result.getData());
        assertEquals(3, counter.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void throwsAfterAllAttemptsExhausted() {
        RetryBehavior<IRequest<Response<String>>, Response<String>>
                behavior = new RetryBehavior<>(2, 10, 1.0);
        AtomicInteger counter = new AtomicInteger(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> behavior.handle(
                        (IRequest<Response<String>>)
                                (IRequest<?>) new TestCmd(),
                        () -> {
                            counter.incrementAndGet();
                            throw new RuntimeException("boom");
                        }));

        assertEquals("boom", ex.getMessage());
        assertEquals(2, counter.get());
    }

    @Test
    void computeDelayUsesExponentialBackoff() {
        RetryBehavior<?, ?> behavior =
                new RetryBehavior<>(5, 100, 2.0);
        assertEquals(100, behavior.computeDelay(1));
        assertEquals(200, behavior.computeDelay(2));
        assertEquals(400, behavior.computeDelay(3));
    }

    @Test
    void orderIsLowestPrecedenceMinus100() {
        RetryBehavior<?, ?> behavior =
                new RetryBehavior<>(3, 100, 2.0);
        assertEquals(Ordered.LOWEST_PRECEDENCE - 100,
                behavior.getOrder());
    }

    @Test
    @SuppressWarnings("unchecked")
    void singleAttemptDoesNotRetry() {
        RetryBehavior<IRequest<Response<String>>, Response<String>>
                behavior = new RetryBehavior<>(1, 10, 2.0);
        AtomicInteger counter = new AtomicInteger(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> behavior.handle(
                        (IRequest<Response<String>>)
                                (IRequest<?>) new TestCmd(),
                        () -> {
                            counter.incrementAndGet();
                            throw new RuntimeException("once");
                        }));

        assertEquals(1, counter.get());
        assertEquals("once", ex.getMessage());
    }
}
