package io.github.valossa515.spring_courier.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for broker delivery, under the
 * {@code spring.courier.messaging} prefix.
 *
 * <pre>
 *   spring.courier.messaging.enabled=true
 *   spring.courier.messaging.topic-prefix=courier.
 *   spring.courier.messaging.topic=
 *   spring.courier.messaging.send-timeout-ms=10000
 * </pre>
 */
@ConfigurationProperties(prefix = "spring.courier.messaging")
public class MessagingProperties {

    /**
     * Master switch. Disabled by default: turning it on redirects the outbox
     * away from in-process handlers and onto the broker, which is a change of
     * destination rather than an addition.
     */
    private boolean enabled = false;

    /** Prepended to the event's simple class name to form the topic. */
    private String topicPrefix = "courier.";

    /** When set, every event goes to this topic and the prefix is ignored. */
    private String topic;

    /**
     * How long to wait for the broker acknowledgement before treating the send
     * as failed. The poller cannot mark a message delivered until this
     * returns, so the value bounds how long one message can stall a poll cycle.
     */
    private long sendTimeoutMs = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getSendTimeoutMs() {
        return sendTimeoutMs;
    }

    public void setSendTimeoutMs(long sendTimeoutMs) {
        this.sendTimeoutMs = sendTimeoutMs;
    }
}
