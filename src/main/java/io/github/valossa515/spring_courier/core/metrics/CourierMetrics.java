package io.github.valossa515.spring_courier.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Centralizes Micrometer metric recording for the Spring Courier CQRS pipeline.
 *
 * <p>Recorded metrics:
 * <ul>
 *   <li>{@code courier.request} — timer with tags {@code type} (request class)
 *       and {@code outcome} (success / error)</li>
 *   <li>{@code courier.request.error} — counter with tags {@code type} and
 *       {@code exception}</li>
 *   <li>{@code courier.notification} — counter with tag {@code type}
 *       (notification class)</li>
 * </ul>
 */
public class CourierMetrics {

    private static final String TAG_TYPE = "type";
    private static final String TAG_OUTCOME = "outcome";
    private static final String TAG_EXCEPTION = "exception";

    private final MeterRegistry registry;

    public CourierMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * Records a successful request execution.
     *
     * @param requestType simple class name of the request
     * @param durationNs  elapsed time in nanoseconds
     */
    public void recordSuccess(String requestType, long durationNs) {
        timer(requestType, "success").record(durationNs, TimeUnit.NANOSECONDS);
    }

    /**
     * Records a failed request execution.
     *
     * @param requestType   simple class name of the request
     * @param durationNs    elapsed time in nanoseconds
     * @param exceptionName simple class name of the exception (or {@code "none"})
     */
    public void recordError(String requestType, long durationNs, String exceptionName) {
        timer(requestType, "error").record(durationNs, TimeUnit.NANOSECONDS);
        Counter.builder("courier.request.error")
                .description("Errors during request handling")
                .tag(TAG_TYPE, requestType)
                .tag(TAG_EXCEPTION, exceptionName != null ? exceptionName : "unknown")
                .register(registry)
                .increment();
    }

    /**
     * Increments the notification counter.
     *
     * @param notificationType simple class name of the notification
     */
    public void recordNotification(String notificationType) {
        Counter.builder("courier.notification")
                .description("Notifications published")
                .tag(TAG_TYPE, notificationType)
                .register(registry)
                .increment();
    }

    private Timer timer(String requestType, String outcome) {
        return Timer.builder("courier.request")
                .description("Request execution time")
                .tag(TAG_TYPE, requestType)
                .tag(TAG_OUTCOME, outcome)
                .register(registry);
    }
}
