package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierPropertiesNewFeaturesTest {

    @Test
    void defaultNotificationStrategyIsSequential() {
        CourierProperties props = new CourierProperties();
        assertEquals(NotificationPublishingStrategy.SEQUENTIAL,
                props.getNotificationStrategy());
    }

    @Test
    void canSetNotificationStrategy() {
        CourierProperties props = new CourierProperties();
        props.setNotificationStrategy(
                NotificationPublishingStrategy.PARALLEL_WHEN_ALL);
        assertEquals(NotificationPublishingStrategy.PARALLEL_WHEN_ALL,
                props.getNotificationStrategy());
    }

    @Test
    void defaultLoggingEnabledIsTrue() {
        CourierProperties props = new CourierProperties();
        assertTrue(props.getLogging().isEnabled());
    }

    @Test
    void canDisableLogging() {
        CourierProperties props = new CourierProperties();
        props.getLogging().setEnabled(false);
        assertFalse(props.getLogging().isEnabled());
    }

    @Test
    void canSetLoggingViaSetterObject() {
        CourierProperties props = new CourierProperties();
        CourierProperties.Logging logging =
                new CourierProperties.Logging();
        logging.setEnabled(false);
        props.setLogging(logging);
        assertFalse(props.getLogging().isEnabled());
    }
}
