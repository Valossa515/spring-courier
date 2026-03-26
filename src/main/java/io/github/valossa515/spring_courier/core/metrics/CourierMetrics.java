package io.github.valossa515.spring_courier.core.metrics;

/**
 * Metric name and tag constants used by {@link MeteredCourier} for
 * Micrometer instrumentation.
 *
 * <p>All metric names follow the Micrometer dot-notation convention
 * (e.g. {@code courier.send.duration}). Prometheus will automatically
 * convert dots to underscores and append the unit suffix.
 */
public final class CourierMetrics {

    private CourierMetrics() {
    }

    // ── Timers ──────────────────────────────────────────────────────────
    public static final String SEND_DURATION = "courier.send.duration";
    public static final String SEND_ASYNC_DURATION = "courier.send.async.duration";
    public static final String PUBLISH_DURATION = "courier.publish.duration";
    public static final String PUBLISH_ASYNC_DURATION = "courier.publish.async.duration";

    // ── Counters ────────────────────────────────────────────────────────
    public static final String SEND_TOTAL = "courier.send";
    public static final String PUBLISH_TOTAL = "courier.publish";
    public static final String HANDLER_ERRORS = "courier.handler.errors";
    public static final String HANDLER_TIMEOUTS = "courier.handler.timeouts";
    public static final String VALIDATION_FAILURES = "courier.validation.failures";

    public static final String CACHE_HITS = "courier.cache.hits";
    public static final String CACHE_MISSES = "courier.cache.misses";
    public static final String RETRY_ATTEMPTS = "courier.retry.attempts";
    public static final String RETRY_EXHAUSTED = "courier.retry.exhausted";
    public static final String IDEMPOTENCY_HITS = "courier.idempotency.hits";
    public static final String IDEMPOTENCY_MISSES = "courier.idempotency.misses";
    public static final String BATCH_SEND_DURATION = "courier.batch.send.duration";
    public static final String BATCH_SEND_SIZE = "courier.batch.send.size";

    // ── Long Task Timers ──────────────────────────────────────────────
    public static final String IN_FLIGHT_REQUESTS = "courier.requests.in.flight";

    // ── Gauges ──────────────────────────────────────────────────────────
    public static final String HANDLERS_REGISTERED = "courier.handlers.registered";
    public static final String NOTIFICATION_HANDLERS_REGISTERED =
            "courier.notification.handlers.registered";
    public static final String PIPELINE_BEHAVIORS_REGISTERED =
            "courier.pipeline.behaviors.registered";

    // ── Tag keys ────────────────────────────────────────────────────────
    public static final String TAG_REQUEST_TYPE = "request.type";
    public static final String TAG_REQUEST_CATEGORY = "request.category";
    public static final String TAG_HANDLER_TYPE = "handler.type";
    public static final String TAG_NOTIFICATION_TYPE = "notification.type";
    public static final String TAG_OUTCOME = "outcome";
    public static final String TAG_EXCEPTION_TYPE = "exception.type";

    // ── Tag values ──────────────────────────────────────────────────────
    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_ERROR = "error";
    public static final String CATEGORY_COMMAND = "command";
    public static final String CATEGORY_QUERY = "query";
    public static final String CATEGORY_GENERIC = "request";
}
