package io.github.valossa515.spring_courier.messaging;

import io.github.valossa515.spring_courier.outbox.OutboxDispatcher;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Auto-configuration that sends drained outbox messages to Kafka instead of
 * republishing them in-process.
 *
 * <p>Activates when {@code spring.courier.messaging.enabled=true} and Spring
 * Kafka is on the classpath. The {@link OutboxDispatcher} bean defined here
 * replaces the outbox module's in-process default, which is
 * {@code @ConditionalOnMissingBean} — so nothing else has to change.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "spring.courier.messaging", name = "enabled",
        havingValue = "true")
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingAutoConfiguration {

    /** Bean name of the template used to publish outbox messages. */
    public static final String TEMPLATE_BEAN = "courierKafkaTemplate";

    /**
     * Template dedicated to the outbox: String key and value, because the
     * payload is already the serialized JSON held in the outbox row.
     *
     * <p>Durability settings are not left to defaults. {@code acks=all} plus an
     * idempotent producer are what make the handoff safe: the outbox exists so
     * an event is never lost, and acknowledging on the leader alone would give
     * that guarantee away at the last step. Idempotence stops a producer-side
     * retry from turning one event into two duplicates on the topic.
     *
     * <p>Named distinctly so it never collides with the application's own
     * {@code kafkaTemplate}, and {@link ConditionalOnMissingBean} so an
     * application can supply its own.
     */
    @Bean(TEMPLATE_BEAN)
    @ConditionalOnMissingBean(name = TEMPLATE_BEAN)
    public KafkaTemplate<String, String> courierKafkaTemplate(
            @Value("${spring.courier.messaging.bootstrap-servers:"
                    + "${spring.kafka.bootstrap-servers:localhost:9092}}")
            String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
    }

    @Bean
    @ConditionalOnMissingBean(OutboxTopicResolver.class)
    public OutboxTopicResolver outboxTopicResolver(MessagingProperties properties) {
        return new EventTypeTopicResolver(properties.getTopicPrefix(), properties.getTopic());
    }

    /**
     * Replaces the outbox's in-process dispatcher with the Kafka one.
     */
    @Bean
    @ConditionalOnMissingBean(OutboxDispatcher.class)
    public OutboxDispatcher kafkaOutboxDispatcher(
            @Qualifier(TEMPLATE_BEAN) KafkaTemplate<String, String> template,
            OutboxTopicResolver topicResolver,
            MessagingProperties properties) {
        return new KafkaOutboxDispatcher(template, topicResolver,
                Duration.ofMillis(properties.getSendTimeoutMs()));
    }
}
