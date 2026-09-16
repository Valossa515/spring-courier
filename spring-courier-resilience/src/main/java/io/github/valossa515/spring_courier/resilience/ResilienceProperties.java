package io.github.valossa515.spring_courier.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the resilience behaviors, under the
 * {@code spring.courier.resilience} prefix.
 *
 * <pre>
 *   spring.courier.resilience.enabled=true
 *   spring.courier.resilience.circuit-breaker-enabled=true
 *   spring.courier.resilience.rate-limiter-enabled=false
 *   spring.courier.resilience.bulkhead-enabled=false
 * </pre>
 *
 * <p>These switches only decide which behaviors are wired. How each one
 * behaves is configured with the standard Resilience4j properties, per request
 * type, e.g. {@code resilience4j.circuitbreaker.instances.<RequestType>.*}.
 */
@ConfigurationProperties(prefix = "spring.courier.resilience")
public class ResilienceProperties {

    /** Master switch for the module. Disabled by default. */
    private boolean enabled = false;

    /**
     * Circuit breaker. On by default once the module is enabled: it only
     * intervenes after a dependency is already failing, so it cannot throttle
     * a healthy system.
     */
    private boolean circuitBreakerEnabled = true;

    /**
     * Rate limiter. Off by default on purpose — it imposes a hard ceiling
     * (Resilience4j's own defaults are deliberately small), so it should be a
     * conscious choice with a configured limit rather than something a master
     * switch turns on underneath you.
     */
    private boolean rateLimiterEnabled = false;

    /**
     * Bulkhead. Off by default for the same reason as the rate limiter: its
     * default concurrency cap would silently bound throughput.
     */
    private boolean bulkheadEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCircuitBreakerEnabled() {
        return circuitBreakerEnabled;
    }

    public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) {
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }

    public boolean isRateLimiterEnabled() {
        return rateLimiterEnabled;
    }

    public void setRateLimiterEnabled(boolean rateLimiterEnabled) {
        this.rateLimiterEnabled = rateLimiterEnabled;
    }

    public boolean isBulkheadEnabled() {
        return bulkheadEnabled;
    }

    public void setBulkheadEnabled(boolean bulkheadEnabled) {
        this.bulkheadEnabled = bulkheadEnabled;
    }
}
