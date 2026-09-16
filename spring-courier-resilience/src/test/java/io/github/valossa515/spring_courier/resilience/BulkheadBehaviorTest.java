package io.github.valossa515.spring_courier.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.valossa515.spring_courier.core.pipelines.BehaviorMetrics;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

class BulkheadBehaviorTest {

    /** RetryBehavior's order in core; the bulkhead must sit inside it. */
    private static final int RETRY_ORDER = Ordered.HIGHEST_PRECEDENCE + 150;

    private final SampleRequest request = new SampleRequest("r-1");

    private BulkheadRegistry singleSlot() {
        return BulkheadRegistry.of(BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build());
    }

    @Test
    void allowsCallsWhileSlotsAreFree() {
        var behavior = new BulkheadBehavior<SampleRequest, String>(singleSlot(), BehaviorMetrics.NOOP);

        assertThat(behavior.handle(request, () -> "ok")).isEqualTo("ok");
    }

    @Test
    void rejectsWhenSaturated() {
        BulkheadRegistry registry = singleSlot();
        var behavior = new BulkheadBehavior<SampleRequest, String>(registry, BehaviorMetrics.NOOP);

        // Hold the only slot, as a concurrent in-flight call would.
        registry.bulkhead(SampleRequest.class.getSimpleName()).tryAcquirePermission();

        assertThatThrownBy(() -> behavior.handle(request, () -> "blocked"))
                .isInstanceOf(BulkheadFullException.class);
    }

    @Test
    void releasesTheSlotAfterTheCall() {
        var behavior = new BulkheadBehavior<SampleRequest, String>(singleSlot(), BehaviorMetrics.NOOP);

        behavior.handle(request, () -> "first");

        assertThat(behavior.handle(request, () -> "second")).isEqualTo("second");
    }

    @Test
    void runsInsideRetrySoAPermitIsNotHeldAcrossBackoffSleeps() {
        var behavior = new BulkheadBehavior<SampleRequest, String>(singleSlot(), BehaviorMetrics.NOOP);

        assertThat(behavior.getOrder())
                .as("a permit held across the whole retry sequence would starve the pool "
                        + "with calls that are only sleeping")
                .isGreaterThan(RETRY_ORDER);
    }
}
