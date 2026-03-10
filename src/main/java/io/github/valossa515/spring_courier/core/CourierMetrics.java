package io.github.valossa515.spring_courier.core;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer-based instrumentation for the Courier CQRS pipeline.
 *
 * <p>When present, the {@link Courier} records the following metrics:
 * <ul>
 *   <li>{@code courier.request} &ndash; Timer for request latency
 *       (tags: {@code request_type}, {@code status})</li>
 *   <li>{@code courier.request.active} &ndash; Gauge for in-flight requests</li>
 *   <li>{@code courier.notification} &ndash; Timer for notification latency
 *       (tags: {@code notification_type})</li>
 *   <li>{@code courier.handler.timeout} &ndash; Counter for handler timeouts</li>
 *   <li>{@code courier.handler.error} &ndash; Counter for handler errors
 *       (tags: {@code request_type})</li>
 * </ul>
 *
 * <p>These metrics are fully compatible with Prometheus and can be visualized
 * in Grafana dashboards.
 */
public class CourierMetrics {

    private final MeterRegistry registry;
    private final AtomicInteger activeRequests;

    public CourierMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.activeRequests = registry.gauge(
                "courier.request.active",
                new AtomicInteger(0)
        );
    }

    /**
     * Records the latency and outcome of a request dispatched via
     * {@link Courier#send}.
     *
     * @param requestType simple class name of the request
     * @param success     whether the request completed successfully
     * @param durationNs  elapsed time in nanoseconds
     */
    public void recordRequest(String requestType, boolean success,
                              long durationNs) {
        Timer.builder("courier.request")
                .description("Time spent processing courier requests")
                .tag("request_type", requestType)
                .tag("status", success ? "success" : "error")
                .register(registry)
                .record(durationNs, TimeUnit.NANOSECONDS);
    }

    /**
     * Records the latency of a notification published via
     * {@link Courier#publish} or {@link Courier#publishAsync}.
     *
     * @param notificationType simple class name of the notification
     * @param durationNs       elapsed time in nanoseconds
     */
    public void recordNotification(String notificationType,
                                   long durationNs) {
        Timer.builder("courier.notification")
                .description("Time spent publishing courier notifications")
                .tag("notification_type", notificationType)
                .register(registry)
                .record(durationNs, TimeUnit.NANOSECONDS);
    }

    /**
     * Increments the handler timeout counter.
     *
     * @param requestType simple class name of the request that timed out
     */
    public void recordTimeout(String requestType) {
        Counter.builder("courier.handler.timeout")
                .description("Number of handler timeouts")
                .tag("request_type", requestType)
                .register(registry)
                .increment();
    }

    /**
     * Increments the handler error counter.
     *
     * @param requestType simple class name of the request that failed
     */
    public void recordError(String requestType) {
        Counter.builder("courier.handler.error")
                .description("Number of handler errors")
                .tag("request_type", requestType)
                .register(registry)
                .increment();
    }

    /**
     * Increments the in-flight request gauge. Must be paired with
     * {@link #decrementActive()}.
     */
    public void incrementActive() {
        activeRequests.incrementAndGet();
    }

    /**
     * Decrements the in-flight request gauge.
     */
    public void decrementActive() {
        activeRequests.decrementAndGet();
    }

    /**
     * Returns the underlying {@link MeterRegistry}.
     */
    public MeterRegistry getRegistry() {
        return registry;
    }
}
