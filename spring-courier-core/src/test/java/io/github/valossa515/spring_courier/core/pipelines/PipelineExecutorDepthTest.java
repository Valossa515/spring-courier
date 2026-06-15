package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.exceptions.CourierException;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineExecutorDepthTest {

    @Test
    void executeThrowsWhenBehaviorChainExceedsMaxDepth() {
        PipelineRegistry registry = new PipelineRegistry();
        PipelineExecutor executor = new PipelineExecutor(registry);

        // Register more than MAX_PIPELINE_DEPTH (64) behaviors
        for (int i = 0; i < 70; i++) {
            registry.registerBehavior(DepthRequest.class, new PassthroughBehavior());
        }

        DepthRequest request = new DepthRequest();

        CourierException ex = assertThrows(CourierException.class,
                () -> executor.execute(request, () -> "result"));
        assertTrue(ex.getMessage().contains("exceeded maximum depth"));
        assertTrue(ex.getMessage().contains("DepthRequest"));
    }

    private static class DepthRequest implements IRequest<String> { }

    private static class PassthroughBehavior implements PipelineBehavior<DepthRequest, String> {
        @Override
        public String handle(DepthRequest request, Next<String> next) {
            return next.invoke();
        }
    }
}
