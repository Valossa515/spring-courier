package io.github.valossa515.spring_courier.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.valossa515.spring_courier.outbox.OutboxMessage;
import io.github.valossa515.spring_courier.outbox.OutboxStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventTypeTopicResolverTest {

    private static OutboxMessage of(String eventType) {
        return new OutboxMessage("id", eventType, "{}", OutboxStatus.PENDING,
                0, null, Instant.now(), null);
    }

    @Test
    void usesThePrefixAndTheSimpleClassName() {
        var resolver = new EventTypeTopicResolver("courier.", null);

        assertThat(resolver.resolve(of("com.example.orders.OrderCreatedNotification")))
                .as("the package is the producer's business, not the consumer's")
                .isEqualTo("courier.OrderCreatedNotification");
    }

    @Test
    void handlesNestedClasses() {
        var resolver = new EventTypeTopicResolver("courier.", null);

        assertThat(resolver.resolve(of("com.example.Events$OrderCreated")))
                .isEqualTo("courier.OrderCreated");
    }

    @Test
    void handlesTypesInTheDefaultPackage() {
        var resolver = new EventTypeTopicResolver("courier.", null);

        assertThat(resolver.resolve(of("OrderCreated"))).isEqualTo("courier.OrderCreated");
    }

    @Test
    void toleratesAnEmptyPrefix() {
        assertThat(new EventTypeTopicResolver("", null).resolve(of("com.example.Evt")))
                .isEqualTo("Evt");
        assertThat(new EventTypeTopicResolver(null, null).resolve(of("com.example.Evt")))
                .isEqualTo("Evt");
    }

    @Test
    void aFixedTopicOverridesThePrefix() {
        var resolver = new EventTypeTopicResolver("courier.", "domain-events");

        assertThat(resolver.resolve(of("com.example.A"))).isEqualTo("domain-events");
        assertThat(resolver.resolve(of("com.example.B"))).isEqualTo("domain-events");
    }

    @Test
    void aBlankFixedTopicIsIgnored() {
        assertThat(new EventTypeTopicResolver("courier.", "   ").resolve(of("com.example.Evt")))
                .isEqualTo("courier.Evt");
    }
}
