package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.pipelines.TransactionBehavior;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Auto-configuration that wraps command handlers in a Spring transaction
 * when {@code spring-tx} is on the classpath and a
 * {@link PlatformTransactionManager} bean exists.
 *
 * <p>Disabled with {@code spring.courier.transaction.enabled=false}.
 */
@Configuration
@ConditionalOnClass(PlatformTransactionManager.class)
@ConditionalOnBean(PlatformTransactionManager.class)
@ConditionalOnProperty(name = "spring.courier.transaction.enabled",
        havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(CourierAutoConfiguration.class)
public class CourierTransactionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TransactionBehavior.class)
    public TransactionBehavior<?, ?> transactionBehavior(
            PlatformTransactionManager transactionManager) {
        return new TransactionBehavior<>(transactionManager);
    }
}
