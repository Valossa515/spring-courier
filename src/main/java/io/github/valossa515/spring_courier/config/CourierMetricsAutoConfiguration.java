package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.metrics.CourierMetrics;
import io.github.valossa515.spring_courier.core.metrics.MetricsBehavior;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration that registers Micrometer-based metrics beans when
 * {@code micrometer-core} is on the classpath and a {@link MeterRegistry}
 * bean is available.
 *
 * <p>Can be disabled via {@code spring.courier.metrics-enabled=false}.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(prefix = "spring.courier", name = "metrics-enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CourierProperties.class)
public class CourierMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CourierMetrics courierMetrics(MeterRegistry meterRegistry) {
        return new CourierMetrics(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(MetricsBehavior.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public MetricsBehavior<?, ?> metricsBehavior(CourierMetrics courierMetrics) {
        return new MetricsBehavior<>(courierMetrics);
    }
}
