package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.slack.SlackAlertManager;
import io.github.valossa515.spring_courier.core.slack.SlackNotifier;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourierSlackAutoConfigurationTest {

    @Test
    void createsSlackNotifierBean() {
        CourierSlackAutoConfiguration config =
                new CourierSlackAutoConfiguration();
        CourierProperties properties = new CourierProperties();
        properties.getSlack().setWebhookUrl(
                "https://hooks.slack.com/services/T/B/x");
        properties.getSlack().setChannel("#test");

        SlackNotifier notifier = config.slackNotifier(properties);
        assertNotNull(notifier);
    }

    @Test
    void createsSlackAlertManagerBean() {
        CourierSlackAutoConfiguration config =
                new CourierSlackAutoConfiguration();
        CourierProperties properties = new CourierProperties();
        properties.getSlack().setWebhookUrl(
                "https://hooks.slack.com/services/T/B/x");

        SlackNotifier notifier = config.slackNotifier(properties);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        SlackAlertManager manager = config.slackAlertManager(
                registry, notifier, properties);
        assertNotNull(manager);
    }
}
