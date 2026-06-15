package io.github.valossa515.spring_courier.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.valossa515.spring_courier.core.Courier;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
            .withUserConfiguration(Infra.class);

    @Test
    void registersBeansWhenEnabled() {
        runner.withPropertyValues(
                        "spring.courier.outbox.enabled=true",
                        "spring.courier.outbox.auto-create-schema=true",
                        "spring.courier.outbox.poll-delay-ms=3600000")
                .run(ctx -> assertThat(ctx)
                        .hasSingleBean(OutboxPublisher.class)
                        .hasSingleBean(OutboxPoller.class)
                        .hasSingleBean(OutboxStore.class)
                        .hasSingleBean(OutboxSerializer.class)
                        .hasSingleBean(OutboxSchemaInitializer.class));
    }

    @Test
    void backsOffWhenDisabled() {
        runner.run(ctx -> assertThat(ctx)
                .doesNotHaveBean(OutboxPublisher.class)
                .doesNotHaveBean(OutboxPoller.class));
    }

    @Test
    void doesNotCreateSchemaInitializerByDefault() {
        runner.withPropertyValues("spring.courier.outbox.enabled=true",
                        "spring.courier.outbox.poll-delay-ms=3600000")
                .run(ctx -> assertThat(ctx)
                        .hasSingleBean(OutboxPublisher.class)
                        .doesNotHaveBean(OutboxSchemaInitializer.class));
    }

    @Test
    void honorsCustomOutboxStore() {
        OutboxStore custom = mock(OutboxStore.class);
        runner.withPropertyValues("spring.courier.outbox.enabled=true",
                        "spring.courier.outbox.poll-delay-ms=3600000")
                .withBean(OutboxStore.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(OutboxStore.class)).isSameAs(custom));
    }

    @Configuration(proxyBeanMethods = false)
    static class Infra {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:ac-test;DB_CLOSE_DELAY=-1");
        }

        @Bean
        Courier courier() {
            return mock(Courier.class);
        }
    }
}
