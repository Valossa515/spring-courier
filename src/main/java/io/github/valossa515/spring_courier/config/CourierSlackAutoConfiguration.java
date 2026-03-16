package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.slack.SlackAlertManager;
import io.github.valossa515.spring_courier.core.slack.SlackNotifier;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that enables built-in Slack alerting for Spring Courier
 * metrics. Activates when:
 * <ul>
 *   <li>{@code MeterRegistry} is available (metrics must be enabled)</li>
 *   <li>{@code spring.courier.slack.webhook-url} is set</li>
 *   <li>{@code spring.courier.slack.enabled} is {@code true} (default)</li>
 * </ul>
 *
 * <p>Once active, the library evaluates alert rules on a fixed interval and
 * sends notifications directly to the configured Slack webhook — no external
 * alerting infrastructure (Grafana, Prometheus Alertmanager) is required.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "spring.courier.slack.webhook-url")
@AutoConfigureAfter(CourierMetricsAutoConfiguration.class)
public class CourierSlackAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(CourierSlackAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.courier.slack.enabled",
            havingValue = "true", matchIfMissing = true)
    public SlackNotifier slackNotifier(CourierProperties properties) {
        CourierProperties.Slack slack = properties.getSlack();
        logger.info("Configuring Slack notifier for channel: {}",
                slack.getChannel());
        return new SlackNotifier(slack.getWebhookUrl(), slack.getChannel());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.courier.slack.enabled",
            havingValue = "true", matchIfMissing = true)
    public SlackAlertManager slackAlertManager(MeterRegistry meterRegistry,
                                               SlackNotifier slackNotifier,
                                               CourierProperties properties) {
        return new SlackAlertManager(
                meterRegistry, slackNotifier, properties.getSlack());
    }
}
