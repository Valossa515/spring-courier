package io.github.valossa515.spring_courier.messaging;

import io.github.valossa515.spring_courier.outbox.OutboxMessage;

/**
 * Default {@link OutboxTopicResolver}: one topic per event type, named
 * {@code <prefix><SimpleClassName>} — e.g. {@code courier.OrderCreatedNotification}.
 *
 * <p>The simple name is used rather than the fully qualified one so the topic
 * does not leak the producer's package structure, which consumers in other
 * services should not have to mirror.
 *
 * <p>A fixed topic can be configured instead, in which case every event lands
 * on the same one.
 */
public class EventTypeTopicResolver implements OutboxTopicResolver {

    private final String prefix;
    private final String fixedTopic;

    /**
     * Creates a resolver.
     *
     * @param prefix     prepended to the event's simple class name
     * @param fixedTopic when non-blank, every message goes to this topic and
     *                   the prefix is ignored
     */
    public EventTypeTopicResolver(String prefix, String fixedTopic) {
        this.prefix = prefix == null ? "" : prefix;
        this.fixedTopic = fixedTopic;
    }

    @Override
    public String resolve(OutboxMessage message) {
        if (fixedTopic != null && !fixedTopic.isBlank()) {
            return fixedTopic;
        }
        return prefix + simpleName(message.eventType());
    }

    private static String simpleName(String fullyQualified) {
        int lastDot = fullyQualified.lastIndexOf('.');
        String name = lastDot >= 0 ? fullyQualified.substring(lastDot + 1) : fullyQualified;
        // Nested types arrive as Outer$Inner; keep only the innermost name.
        int lastDollar = name.lastIndexOf('$');
        return lastDollar >= 0 ? name.substring(lastDollar + 1) : name;
    }
}
