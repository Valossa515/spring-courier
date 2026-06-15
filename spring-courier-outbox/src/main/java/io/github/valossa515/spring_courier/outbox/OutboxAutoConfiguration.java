package io.github.valossa515.spring_courier.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.valossa515.spring_courier.core.Courier;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Auto-configuration for the Spring Courier transactional outbox.
 *
 * <p>Activates when {@code spring.courier.outbox.enabled=true}, a JDBC
 * {@link DataSource} and a {@link Courier} bean are present, and JDBC is on the
 * classpath. Every bean is {@link ConditionalOnMissingBean} so applications can
 * override any piece (e.g. provide a custom {@link OutboxStore}).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({Courier.class, JdbcTemplate.class, DataSource.class})
@ConditionalOnBean({Courier.class, DataSource.class})
@ConditionalOnProperty(prefix = "spring.courier.outbox", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxSerializer outboxSerializer(ObjectProvider<ObjectMapper> objectMapper) {
        return new OutboxSerializer(objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxStore outboxStore(DataSource dataSource, OutboxProperties properties) {
        return new JdbcOutboxStore(dataSource, properties.getTableName());
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxPublisher outboxPublisher(OutboxStore store, OutboxSerializer serializer) {
        return new OutboxPublisher(store, serializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxPoller outboxPoller(OutboxStore store, OutboxSerializer serializer,
            Courier courier, OutboxProperties properties) {
        return new OutboxPoller(store, serializer, courier, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.courier.outbox", name = "auto-create-schema",
            havingValue = "true")
    @ConditionalOnMissingBean
    public OutboxSchemaInitializer outboxSchemaInitializer(DataSource dataSource,
            OutboxProperties properties) {
        return new OutboxSchemaInitializer(dataSource, properties.getTableName());
    }
}
