package io.github.valossa515.spring_courier.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
