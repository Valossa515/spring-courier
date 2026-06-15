package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierStressTest {

    private CourierTestFixture fixture;
    private CountingStressHandler handler;

    @BeforeEach
    void setUp() {
        fixture = CourierTestFixture.create();
        handler = new CountingStressHandler();
        fixture.handlerRegistry().registerHandler(StressRequest.class, handler);
    }

    @Test
    void courierHandlesThousandsOfSequentialRequests() {
        int totalRequests = 5_000;

        IntStream.range(0, totalRequests)
                .mapToObj(StressRequest::new)
                .map(fixture.courier()::send)
                .forEach(response -> {
                    assertTrue(response.isSuccess());
                    assertTrue(response.getData().startsWith("ok-"));
                });

        assertEquals(totalRequests, handler.processedCount());
        assertEquals(totalRequests, handler.uniqueRequests());
    }

    @Test
    void courierHandlesConcurrentBurstOfRequests() throws Exception {
        int totalRequests = 2_000;
        ExecutorService executor = Executors.newFixedThreadPool(16);

        List<Callable<Response<String>>> tasks = IntStream.range(0, totalRequests)
                .mapToObj(id -> (Callable<Response<String>>) () -> fixture.courier().send(new StressRequest(id)))
                .toList();

        List<Future<Response<String>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        for (Future<Response<String>> future : futures) {
            Response<String> response = future.get(5, TimeUnit.SECONDS);
            assertTrue(response.isSuccess());
            assertTrue(response.getData().startsWith("ok-"));
        }

        assertEquals(totalRequests, handler.processedCount());
        assertEquals(totalRequests, handler.uniqueRequests());
    }

    private static class CountingStressHandler {
        private final AtomicInteger processed = new AtomicInteger();
        private final ConcurrentSkipListSet<Integer> requests = new ConcurrentSkipListSet<>();

        public Response<String> handle(StressRequest request) {
            requests.add(request.id());
            processed.incrementAndGet();
            return Response.success("ok-" + request.id());
        }

        int processedCount() {
            return processed.get();
        }

        int uniqueRequests() {
            return requests.size();
        }
    }

    private record StressRequest(int id) implements IRequest<String> { }
}
