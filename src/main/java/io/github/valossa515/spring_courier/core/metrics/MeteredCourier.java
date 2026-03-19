package io.github.valossa515.spring_courier.core.metrics;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.exceptions.ValidationException;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static io.github.valossa515.spring_courier.core.metrics.CourierMetrics.*;

/**
 * Micrometer-instrumented extension of {@link Courier} that records
 * latency histograms (with p50/p95/p99 percentiles), throughput counters,
 * and error counters for every dispatched request and notification.
 *
 * <p>This class is activated automatically by
 * {@code CourierMetricsAutoConfiguration} when {@code micrometer-core}
 * is on the classpath and {@code spring.courier.metrics.enabled=true}
 * (the default).
 */
public class MeteredCourier extends Courier {

    private final MeterRegistry registry;
    private final Counter timeoutCounter;

    public MeteredCourier(@NotNull HandlerRegistry handlerRegistry,
                          @NotNull NotificationRegistry notificationRegistry,
                          @NotNull PipelineExecutor pipelineExecutor,
                          @NotNull PipelineRegistry pipelineRegistry,
                          Executor asyncExecutor,
                          long asyncTimeoutMs,
                          @NotNull MeterRegistry meterRegistry) {
        super(handlerRegistry, notificationRegistry, pipelineExecutor,
                asyncExecutor, asyncTimeoutMs);
        this.registry = meterRegistry;
        this.timeoutCounter = Counter.builder(HANDLER_TIMEOUTS)
                .description("Number of handler timeouts")
                .register(meterRegistry);
        registerGauges(handlerRegistry, notificationRegistry, pipelineRegistry);
    }

    @Override
    public <R> Response<R> send(@NotNull IRequest<R> request) {
        Timer.Sample sample = Timer.start(registry);
        String requestType = request.getClass().getSimpleName();
        String category = resolveCategory(request);

        try {
            Response<R> response = super.send(request);

            String outcome = response.isSuccess()
                    ? OUTCOME_SUCCESS : OUTCOME_ERROR;

            sample.stop(Timer.builder(SEND_DURATION)
                    .description("Time spent dispatching requests")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .tag(TAG_REQUEST_TYPE, requestType)
                    .tag(TAG_REQUEST_CATEGORY, category)
                    .tag(TAG_OUTCOME, outcome)
                    .register(registry));

            Counter.builder(SEND_TOTAL)
                    .description("Total requests dispatched")
                    .tag(TAG_REQUEST_TYPE, requestType)
                    .tag(TAG_OUTCOME, outcome)
                    .register(registry).increment();

            if (!response.isSuccess()) {
                if (response.isValidationFailure()) {
                    Counter.builder(VALIDATION_FAILURES)
                            .description("Number of validation failures")
                            .tag(TAG_REQUEST_TYPE, requestType)
                            .register(registry).increment();
                } else {
                    String exType = response.getExceptionType();
                    Counter.builder(HANDLER_ERRORS)
                            .description("Number of handler errors")
                            .tag(TAG_REQUEST_TYPE, requestType)
                            .tag(TAG_EXCEPTION_TYPE,
                                    exType != null && !exType.isBlank()
                                            ? exType : "unknown")
                            .register(registry).increment();
                }

                if (response.getStatusCode() == 504) {
                    timeoutCounter.increment();
                }
            }

            return response;

        } catch (ValidationException ex) {
            sample.stop(Timer.builder(SEND_DURATION)
                    .description("Time spent dispatching requests")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .tag(TAG_REQUEST_TYPE, requestType)
                    .tag(TAG_REQUEST_CATEGORY, category)
                    .tag(TAG_OUTCOME, OUTCOME_ERROR)
                    .register(registry));

            Counter.builder(SEND_TOTAL)
                    .description("Total requests dispatched")
                    .tag(TAG_REQUEST_TYPE, requestType)
                    .tag(TAG_OUTCOME, OUTCOME_ERROR)
                    .register(registry).increment();

            Counter.builder(VALIDATION_FAILURES)
                    .description("Number of validation failures")
                    .tag(TAG_REQUEST_TYPE, requestType)
                    .register(registry).increment();

            throw ex;

        } catch (RuntimeException ex) {
            sample.stop(Timer.builder(SEND_DURATION)
                    .description("Time spent dispatching requests")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .tag(TAG_REQUEST_TYPE, requestType)
                    .tag(TAG_REQUEST_CATEGORY, category)
                    .tag(TAG_OUTCOME, OUTCOME_ERROR)
                    .register(registry));

            Counter.builder(SEND_TOTAL)
                    .description("Total requests dispatched")
                    .tag(TAG_REQUEST_TYPE, requestType)
                    .tag(TAG_OUTCOME, OUTCOME_ERROR)
                    .register(registry).increment();

            Counter.builder(HANDLER_ERRORS)
                    .description("Number of handler errors")
                    .tag(TAG_REQUEST_TYPE, requestType)
                    .tag(TAG_EXCEPTION_TYPE, ex.getClass().getSimpleName())
                    .register(registry).increment();

            throw ex;
        }
    }

    @Override
    public void publish(@NotNull INotification notification) {
        Timer.Sample sample = Timer.start(registry);
        String notificationType = notification.getClass().getSimpleName();

        super.publish(notification);

        sample.stop(Timer.builder(PUBLISH_DURATION)
                .description("Time spent publishing notifications")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag(TAG_NOTIFICATION_TYPE, notificationType)
                .register(registry));

        Counter.builder(PUBLISH_TOTAL)
                .description("Total notifications published")
                .tag(TAG_NOTIFICATION_TYPE, notificationType)
                .register(registry).increment();
    }

    @Override
    public CompletableFuture<Void> publishAsync(@NotNull INotification notification) {
        Timer.Sample sample = Timer.start(registry);
        String notificationType = notification.getClass().getSimpleName();

        return super.publishAsync(notification).whenComplete((result, ex) -> {
            sample.stop(Timer.builder(PUBLISH_ASYNC_DURATION)
                    .description("Time spent publishing notifications asynchronously")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .tag(TAG_NOTIFICATION_TYPE, notificationType)
                    .register(registry));
            // Note: PUBLISH_TOTAL counter is already incremented by publish()
            // which is called internally by Courier.publishAsync().
        });
    }

    private void registerGauges(HandlerRegistry handlerRegistry,
                                NotificationRegistry notificationRegistry,
                                PipelineRegistry pipelineRegistry) {
        registry.gauge(HANDLERS_REGISTERED,
                handlerRegistry, HandlerRegistry::getHandlerCount);
        registry.gauge(NOTIFICATION_HANDLERS_REGISTERED,
                notificationRegistry, NotificationRegistry::getHandlerCount);
        registry.gauge(PIPELINE_BEHAVIORS_REGISTERED,
                pipelineRegistry, PipelineRegistry::getBehaviorCount);
    }

    private String resolveCategory(IRequest<?> request) {
        return switch (request) {
            case ICommand<?> ignored -> CATEGORY_COMMAND;
            case IQuery<?> ignored   -> CATEGORY_QUERY;
            default                  -> CATEGORY_GENERIC;
        };
    }
}
