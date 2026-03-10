package io.github.valossa515.spring_courier.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierPropertiesTest {

    @Test
    void defaultAsyncTimeoutIs30Seconds() {
        CourierProperties properties = new CourierProperties();
        assertEquals(30_000, properties.getAsyncTimeoutMs());
    }

    @Test
    void asyncTimeoutCanBeCustomized() {
        CourierProperties properties = new CourierProperties();
        properties.setAsyncTimeoutMs(60_000);
        assertEquals(60_000, properties.getAsyncTimeoutMs());
    }

    @Test
    void asyncTimeoutRejectsBelowMinimum() {
        CourierProperties properties = new CourierProperties();
        assertThrows(IllegalArgumentException.class, () -> properties.setAsyncTimeoutMs(50));
    }

    @Test
    void asyncTimeoutRejectsAboveMaximum() {
        CourierProperties properties = new CourierProperties();
        assertThrows(IllegalArgumentException.class, () -> properties.setAsyncTimeoutMs(700_000));
    }

    @Test
    void asyncTimeoutAcceptsBoundaryValues() {
        CourierProperties properties = new CourierProperties();
        properties.setAsyncTimeoutMs(100);
        assertEquals(100, properties.getAsyncTimeoutMs());

        properties.setAsyncTimeoutMs(600_000);
        assertEquals(600_000, properties.getAsyncTimeoutMs());
    }

    @Test
    void metricsEnabledByDefault() {
        CourierProperties properties = new CourierProperties();
        assertTrue(properties.isMetricsEnabled());
    }

    @Test
    void metricsCanBeDisabled() {
        CourierProperties properties = new CourierProperties();
        properties.setMetricsEnabled(false);
        assertFalse(properties.isMetricsEnabled());
    }
}
