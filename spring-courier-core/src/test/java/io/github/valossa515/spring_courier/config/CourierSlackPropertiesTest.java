package io.github.valossa515.spring_courier.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourierSlackPropertiesTest {

    @Test
    void slackDefaultsDisabledWithoutWebhookUrl() {
        CourierProperties properties = new CourierProperties();
        CourierProperties.Slack slack = properties.getSlack();
        assertNull(slack.getWebhookUrl());
        assertTrue(slack.isEnabled());
    }

    @Test
    void slackPropertyDefaults() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        assertEquals("Spring Courier", slack.getAppName());
        assertEquals(60, slack.getEvaluationIntervalSeconds());
        assertEquals(15, slack.getCooldownMinutes());
        assertEquals(300, slack.getForDurationSeconds());
        assertNull(slack.getChannel());
    }

    @Test
    void slackThresholdDefaults() {
        CourierProperties.Slack.Thresholds t =
                new CourierProperties.Slack.Thresholds();
        assertEquals(0.05, t.getErrorRatio(), 0.001);
        assertEquals(1.0, t.getP99LatencySeconds(), 0.001);
        assertEquals(10.0, t.getValidationRate(), 0.001);
        assertEquals(0.5, t.getThroughputDropRatio(), 0.001);
    }

    @Test
    void slackThresholdsCanBeCustomized() {
        CourierProperties.Slack.Thresholds t =
                new CourierProperties.Slack.Thresholds();
        t.setErrorRatio(0.10);
        t.setP99LatencySeconds(2.0);
        t.setValidationRate(20.0);
        t.setThroughputDropRatio(0.3);

        assertEquals(0.10, t.getErrorRatio(), 0.001);
        assertEquals(2.0, t.getP99LatencySeconds(), 0.001);
        assertEquals(20.0, t.getValidationRate(), 0.001);
        assertEquals(0.3, t.getThroughputDropRatio(), 0.001);
    }

    @Test
    void evaluationIntervalRejectsBelowMinimum() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        assertThrows(IllegalArgumentException.class,
                () -> slack.setEvaluationIntervalSeconds(5));
    }

    @Test
    void evaluationIntervalRejectsAboveMaximum() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        assertThrows(IllegalArgumentException.class,
                () -> slack.setEvaluationIntervalSeconds(3601));
    }

    @Test
    void cooldownRejectsBelowMinimum() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        assertThrows(IllegalArgumentException.class,
                () -> slack.setCooldownMinutes(0));
    }

    @Test
    void cooldownRejectsAboveMaximum() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        assertThrows(IllegalArgumentException.class,
                () -> slack.setCooldownMinutes(1441));
    }

    @Test
    void slackCanBeFullyConfigured() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        slack.setWebhookUrl("https://hooks.slack.com/services/T/B/x");
        slack.setChannel("#alerts");
        slack.setAppName("My App");
        slack.setEnabled(true);
        slack.setEvaluationIntervalSeconds(30);
        slack.setCooldownMinutes(10);
        slack.setForDurationSeconds(120);

        assertEquals("https://hooks.slack.com/services/T/B/x",
                slack.getWebhookUrl());
        assertEquals("#alerts", slack.getChannel());
        assertEquals("My App", slack.getAppName());
        assertTrue(slack.isEnabled());
        assertEquals(30, slack.getEvaluationIntervalSeconds());
        assertEquals(10, slack.getCooldownMinutes());
        assertEquals(120, slack.getForDurationSeconds());
    }

    @Test
    void slackCanBeDisabled() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        slack.setEnabled(false);
        assertFalse(slack.isEnabled());
    }

    @Test
    void setSlackReplacesInstance() {
        CourierProperties properties = new CourierProperties();
        CourierProperties.Slack custom = new CourierProperties.Slack();
        custom.setAppName("Custom");
        properties.setSlack(custom);
        assertEquals("Custom", properties.getSlack().getAppName());
    }

    @Test
    void setThresholdsReplacesInstance() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        CourierProperties.Slack.Thresholds custom =
                new CourierProperties.Slack.Thresholds();
        custom.setErrorRatio(0.20);
        slack.setThresholds(custom);
        assertEquals(0.20, slack.getThresholds().getErrorRatio(), 0.001);
    }

    @Test
    void evaluationIntervalAcceptsBoundaryValues() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        slack.setEvaluationIntervalSeconds(10);
        assertEquals(10, slack.getEvaluationIntervalSeconds());

        slack.setEvaluationIntervalSeconds(3600);
        assertEquals(3600, slack.getEvaluationIntervalSeconds());
    }

    @Test
    void cooldownAcceptsBoundaryValues() {
        CourierProperties.Slack slack = new CourierProperties.Slack();
        slack.setCooldownMinutes(1);
        assertEquals(1, slack.getCooldownMinutes());

        slack.setCooldownMinutes(1440);
        assertEquals(1440, slack.getCooldownMinutes());
    }
}
