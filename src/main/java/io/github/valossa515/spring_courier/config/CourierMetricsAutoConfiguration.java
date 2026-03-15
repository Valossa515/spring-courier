package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.metrics.MeteredCourier;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

/**
 * Auto-configuration that replaces the default {@link Courier} bean with a
 * {@link MeteredCourier} when Micrometer is on the classpath.
 *
 * <p>Activates when:
 * <ul>
 *   <li>{@code MeterRegistry} is available on the classpath</li>
 *   <li>{@code spring.courier.metrics.enabled} is {@code true} (the default)</li>
 * </ul>
 *
 * <p>Because this configuration is ordered <em>before</em>
 * {@link CourierAutoConfiguration}, the {@code MeteredCourier} bean satisfies
 * the {@code @ConditionalOnMissingBean(Courier.class)} guard in the base
 * configuration, preventing a plain {@code Courier} from being created.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "spring.courier.metrics.enabled",
        havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(CourierAutoConfiguration.class)
public class CourierMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Courier.class)
    public MeteredCourier meteredCourier(HandlerRegistry handlerRegistry,
                                         NotificationRegistry notificationRegistry,
                                         PipelineExecutor pipelineExecutor,
                                         PipelineRegistry pipelineRegistry,
                                         CourierProperties properties,
                                         ObjectProvider<Executor> asyncExecutor,
                                         MeterRegistry meterRegistry) {
        return new MeteredCourier(
                handlerRegistry,
                notificationRegistry,
                pipelineExecutor,
                pipelineRegistry,
                asyncExecutor.getIfUnique(),
                properties.getAsyncTimeoutMs(),
                meterRegistry);
    }
}
