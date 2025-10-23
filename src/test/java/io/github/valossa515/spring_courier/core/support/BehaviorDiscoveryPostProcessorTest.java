package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class BehaviorDiscoveryPostProcessorTest {

    private PipelineRegistry pipelineRegistry;
    private BehaviorDiscoveryPostProcessor postProcessor;

    @BeforeEach
    void setUp() {
        pipelineRegistry = mock(PipelineRegistry.class);
        postProcessor = new BehaviorDiscoveryPostProcessor(pipelineRegistry);
    }

    @Test
    void registersPipelineBehaviorWithRequestType() {
        SampleBehavior behavior = new SampleBehavior();

        postProcessor.postProcessAfterInitialization(behavior, "sampleBehavior");

        verify(pipelineRegistry).registerBehavior(SampleRequest.class, behavior);
    }

    @Test
    void ignoresBehaviorsWithoutResolvableRequestType() {
        PipelineBehavior<?, ?> behavior = new GenericBehavior() {
            @Override
            public String handle(IRequest<String> request, Next<String> next) {
                return "";
            }
        };

        postProcessor.postProcessAfterInitialization(behavior, "genericBehavior");

        verify(pipelineRegistry, never()).registerBehavior(any(), any());
    }

    private static class SampleRequest implements IRequest<String> {
    }

    private static class SampleBehavior implements PipelineBehavior<SampleRequest, String> {
        @Override
        public String handle(SampleRequest request, Next<String> next) {
            return next.invoke();
        }
    }

    private abstract static class GenericBehavior implements PipelineBehavior<IRequest<String>, String> {
    }
}
