package io.github.valossa515.spring_courier.messaging;

import io.github.valossa515.spring_courier.outbox.OutboxDispatcher;
import io.github.valossa515.spring_courier.outbox.OutboxMessage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * {@link OutboxDispatcher} that publishes drained outbox messages to Kafka, so
 * events written inside a command's transaction become visible to other
 * services.
 *
 * <p>The stored JSON payload is forwarded <em>as-is</em>: the outbox already
 * holds the serialized event, so there is no deserialize/re-serialize round
 * trip and the consuming service does not need the producer's event classes on
 * its classpath.
 *
 * <p><strong>The send is awaited on purpose.</strong> The poller marks a
 * message {@code PROCESSED} only after {@code dispatch} returns, so this method
 * blocks until the broker acknowledges. Returning as soon as the send is queued
 * would let the poller mark a message delivered that Kafka had not yet
 * accepted, and a crash in that window would lose the event — precisely the
 * failure the outbox exists to prevent.
 *
 * <p>Two headers travel with every record:
 * <ul>
 *   <li>{@value #HEADER_MESSAGE_ID} — the outbox id. Delivery is
 *       at-least-once, so consumers need a stable id to deduplicate on.</li>
 *   <li>{@value #HEADER_EVENT_TYPE} — the original event class, for routing or
 *       deserialization without parsing the payload.</li>
 * </ul>
 *
 * <p>The record key is the outbox id, which spreads load evenly across
 * partitions but gives <em>no per-aggregate ordering</em>. Supply a custom
 * {@link OutboxTopicResolver} and/or your own dispatcher if ordering per
 * entity matters.
 */
public class KafkaOutboxDispatcher implements OutboxDispatcher {

    /** Header carrying the outbox id, for consumer-side deduplication. */
    public static final String HEADER_MESSAGE_ID = "courier-message-id";

    /** Header carrying the original event class name. */
    public static final String HEADER_EVENT_TYPE = "courier-event-type";

    private static final Logger LOG = LoggerFactory.getLogger(KafkaOutboxDispatcher.class);

    private final KafkaTemplate<String, String> template;
    private final OutboxTopicResolver topicResolver;
    private final Duration sendTimeout;

    /**
     * Creates the dispatcher.
     *
     * @param template      template used to produce to Kafka
     * @param topicResolver decides the destination topic
     * @param sendTimeout   how long to wait for the broker acknowledgement
     */
    public KafkaOutboxDispatcher(KafkaTemplate<String, String> template,
            OutboxTopicResolver topicResolver, Duration sendTimeout) {
        this.template = template;
        this.topicResolver = topicResolver;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public void dispatch(OutboxMessage message) {
        String topic = topicResolver.resolve(message);

        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, message.id(), message.payload());
        record.headers().add(HEADER_MESSAGE_ID,
                message.id().getBytes(StandardCharsets.UTF_8));
        record.headers().add(HEADER_EVENT_TYPE,
                message.eventType().getBytes(StandardCharsets.UTF_8));

        try {
            template.send(record).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            LOG.debug("Published outbox message {} ({}) to topic {}",
                    message.id(), message.eventType(), topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OutboxPublishException(
                    "Interrupted while publishing outbox message " + message.id(), e);
        } catch (ExecutionException | TimeoutException e) {
            // Thrown rather than swallowed: the poller records the failed
            // attempt and leaves the message pending for a later retry.
            throw new OutboxPublishException("Failed to publish outbox message "
                    + message.id() + " to topic " + topic, e);
        } catch (RuntimeException e) {
            // send() can also fail synchronously — an unreachable broker makes
            // it throw KafkaException while fetching metadata, before there is
            // any future to await. Wrapping it keeps the failure contract the
            // same on both paths.
            throw new OutboxPublishException("Failed to publish outbox message "
                    + message.id() + " to topic " + topic, e);
        }
    }
}
