package io.github.valossa515.spring_courier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for Spring Courier.
 *
 * <p>Properties can be set via {@code application.properties} / {@code application.yml}
 * using the prefix {@code spring.courier}.
 *
 * <pre>
 *   spring.courier.async-timeout-ms=60000
 * </pre>
 */
@ConfigurationProperties(prefix = "spring.courier")
public class CourierProperties {

    /**
     * Timeout in milliseconds for asynchronous handler execution.
     * Defaults to 30 000 ms (30 seconds).
     */
    private long asyncTimeoutMs = 30_000;

    public long getAsyncTimeoutMs() {
        return asyncTimeoutMs;
    }

    public void setAsyncTimeoutMs(long asyncTimeoutMs) {
        this.asyncTimeoutMs = asyncTimeoutMs;
    }
}
