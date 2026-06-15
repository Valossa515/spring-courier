package io.github.valossa515.spring_courier.core.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourierMetricsTest {

    @Test
    void timerConstantsUseCourierPrefix() {
        assertTrue(CourierMetrics.SEND_DURATION.startsWith("courier."));
        assertTrue(CourierMetrics.PUBLISH_DURATION.startsWith("courier."));
        assertTrue(CourierMetrics.PUBLISH_ASYNC_DURATION.startsWith("courier."));
    }

    @Test
    void counterConstantsUseCourierPrefix() {
        assertTrue(CourierMetrics.SEND_TOTAL.startsWith("courier."));
        assertTrue(CourierMetrics.PUBLISH_TOTAL.startsWith("courier."));
        assertTrue(CourierMetrics.HANDLER_ERRORS.startsWith("courier."));
        assertTrue(CourierMetrics.HANDLER_TIMEOUTS.startsWith("courier."));
        assertTrue(CourierMetrics.VALIDATION_FAILURES.startsWith("courier."));
    }

    @Test
    void gaugeConstantsUseCourierPrefix() {
        assertTrue(CourierMetrics.HANDLERS_REGISTERED.startsWith("courier."));
        assertTrue(CourierMetrics.NOTIFICATION_HANDLERS_REGISTERED.startsWith("courier."));
        assertTrue(CourierMetrics.PIPELINE_BEHAVIORS_REGISTERED.startsWith("courier."));
    }

    @Test
    void constantsAreNotBlank() {
        assertFalse(CourierMetrics.TAG_REQUEST_TYPE.isBlank());
        assertFalse(CourierMetrics.TAG_REQUEST_CATEGORY.isBlank());
        assertFalse(CourierMetrics.TAG_OUTCOME.isBlank());
        assertFalse(CourierMetrics.TAG_NOTIFICATION_TYPE.isBlank());
        assertFalse(CourierMetrics.TAG_EXCEPTION_TYPE.isBlank());
        assertFalse(CourierMetrics.TAG_HANDLER_TYPE.isBlank());
    }
}
