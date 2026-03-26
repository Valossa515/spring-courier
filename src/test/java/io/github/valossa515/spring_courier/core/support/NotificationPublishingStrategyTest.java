package io.github.valossa515.spring_courier.core.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationPublishingStrategyTest {

    @Test
    void hasThreeValues() {
        assertEquals(3,
                NotificationPublishingStrategy.values().length);
    }

    @Test
    void valueOfSequential() {
        assertNotNull(NotificationPublishingStrategy
                .valueOf("SEQUENTIAL"));
    }

    @Test
    void valueOfParallelWhenAll() {
        assertNotNull(NotificationPublishingStrategy
                .valueOf("PARALLEL_WHEN_ALL"));
    }

    @Test
    void valueOfStopOnFirstError() {
        assertNotNull(NotificationPublishingStrategy
                .valueOf("STOP_ON_FIRST_ERROR"));
    }
}
