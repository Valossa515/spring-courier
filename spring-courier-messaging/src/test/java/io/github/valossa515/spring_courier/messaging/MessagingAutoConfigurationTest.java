package io.github.valossa515.spring_courier.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.valossa515.spring_courier.outbox.OutboxDispatcher;
import io.github.valossa515.spring_courier.outbox.OutboxMessage;
import io.github.valossa515.spring_courier.outbox.OutboxStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

class MessagingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessagingAutoConfiguration.class));

    @Test
    void backsOffWhenDisabled() {
        runner.run(ctx -> assertThat(ctx)
                .as("enabling it redirects the outbox away from in-process handlers, "
                        + "so it must never happen by merely adding the jar")
                .doesNotHaveBean(OutboxDispatcher.class)
                .doesNotHaveBean(OutboxTopicResolver.class));
    }

    @Test
    void registersTheKafkaDispatcherWhenEnabled() {
        runner.withPropertyValues("spring.courier.messaging.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDispatcher.class);
                    assertThat(ctx.getBean(OutboxDispatcher.class))
                            .isInstanceOf(KafkaOutboxDispatcher.class);
                    assertThat(ctx).hasBean(MessagingAutoConfiguration.TEMPLATE_BEAN);
                });
    }

    @Test
    void theTemplateIsDurableByDefault() {
        runner.withPropertyValues("spring.courier.messaging.enabled=true")
                .run(ctx -> {
                    var factory = (DefaultKafkaProducerFactory<?, ?>) ctx
                            .getBean(MessagingAutoConfiguration.TEMPLATE_BEAN, KafkaTemplate.class)
                            .getProducerFactory();
                    Map<String, Object> config = factory.getConfigurationProperties();

                    assertThat(config.get(ProducerConfig.ACKS_CONFIG))
                            .as("the outbox exists so an event is never lost; acknowledging "
                                    + "on the leader alone would give that away at the last step")
                            .isEqualTo("all");
                    assertThat(config.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG))
                            .as("a producer-side retry must not turn one event into two")
                            .isEqualTo(true);
                });
    }

    @Test
    void theTopicPrefixIsConfigurable() {
        runner.withPropertyValues(
                        "spring.courier.messaging.enabled=true",
                        "spring.courier.messaging.topic-prefix=orders.")
                .run(ctx -> assertThat(resolve(ctx.getBean(OutboxTopicResolver.class)))
                        .isEqualTo("orders.OrderCreatedNotification"));
    }

    @Test
    void aFixedTopicSendsEverythingToOnePlace() {
        runner.withPropertyValues(
                        "spring.courier.messaging.enabled=true",
                        "spring.courier.messaging.topic=domain-events")
                .run(ctx -> assertThat(resolve(ctx.getBean(OutboxTopicResolver.class)))
                        .isEqualTo("domain-events"));
    }

    @Test
    void anApplicationSuppliedDispatcherWins() {
        runner.withPropertyValues("spring.courier.messaging.enabled=true")
                .withUserConfiguration(CustomDispatcherConfig.class)
                .run(ctx -> assertThat(ctx.getBean(OutboxDispatcher.class))
                        .as("the module must never override a dispatcher the application chose")
                        .isNotInstanceOf(KafkaOutboxDispatcher.class));
    }

    @Test
    void anApplicationSuppliedTemplateWins() {
        runner.withPropertyValues("spring.courier.messaging.enabled=true")
                .withUserConfiguration(CustomTemplateConfig.class)
                .run(ctx -> {
                    var factory = (DefaultKafkaProducerFactory<?, ?>) ctx
                            .getBean(MessagingAutoConfiguration.TEMPLATE_BEAN, KafkaTemplate.class)
                            .getProducerFactory();
                    assertThat(factory.getConfigurationProperties()
                            .get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
                            .isEqualTo("broker.example:9092");
                });
    }

    /**
     * Falls back to {@code spring.kafka.bootstrap-servers} when the module's own
     * property is absent, so an application that already configured Kafka does
     * not have to repeat the address.
     */
    @Test
    void reusesTheApplicationsBootstrapServers() {
        runner.withPropertyValues(
                        "spring.courier.messaging.enabled=true",
                        "spring.kafka.bootstrap-servers=kafka-1:9092,kafka-2:9092")
                .run(ctx -> {
                    var factory = (DefaultKafkaProducerFactory<?, ?>) ctx
                            .getBean(MessagingAutoConfiguration.TEMPLATE_BEAN, KafkaTemplate.class)
                            .getProducerFactory();
                    assertThat(factory.getConfigurationProperties()
                            .get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
                            .isEqualTo("kafka-1:9092,kafka-2:9092");
                });
    }

    private static String resolve(OutboxTopicResolver resolver) {
        return resolver.resolve(new OutboxMessage("id", "com.example.OrderCreatedNotification",
                "{}", OutboxStatus.PENDING, 0, null, Instant.now(), null));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDispatcherConfig {

        @Bean
        OutboxDispatcher applicationDispatcher() {
            return message -> {
                // no-op
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTemplateConfig {

        @Bean(MessagingAutoConfiguration.TEMPLATE_BEAN)
        KafkaTemplate<String, String> applicationTemplate() {
            Map<String, Object> config = new HashMap<>();
            config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "broker.example:9092");
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
        }
    }
}
