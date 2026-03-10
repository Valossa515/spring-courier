package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.CourierMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration that registers a {@link CourierMetrics} bean when
 * Micrometer is on the classpath and a {@link MeterRegistry} is available.
 *
 * <p>Metrics can be disabled via {@code spring.courier.metrics-enabled=false}.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(
        prefix = "spring.courier",
        name = "metrics-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(CourierProperties.class)
public class CourierMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    public CourierMetrics courierMetrics(MeterRegistry meterRegistry) {
        return new CourierMetrics(meterRegistry);
    }
}
