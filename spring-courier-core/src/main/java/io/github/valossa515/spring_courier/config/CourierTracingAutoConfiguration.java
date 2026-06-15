package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.pipelines.TracingBehavior;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for OpenTelemetry tracing integration.
 *
 * <p>Activates only when:
 * <ol>
 *   <li>{@code io.opentelemetry.api.trace.Tracer} is on the classpath</li>
 *   <li>A {@link Tracer} bean exists in the context</li>
 *   <li>{@code spring.courier.tracing.enabled} is not {@code false}</li>
 * </ol>
 */
@Configuration
@ConditionalOnClass(Tracer.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(name = "spring.courier.tracing.enabled",
        havingValue = "true", matchIfMissing = true)
public class CourierTracingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TracingBehavior.class)
    public TracingBehavior<?, ?> tracingBehavior(Tracer tracer) {
        return new TracingBehavior<>(tracer);
    }
}
