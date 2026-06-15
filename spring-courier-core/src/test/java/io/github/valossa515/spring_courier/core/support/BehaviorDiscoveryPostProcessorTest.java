package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
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
    void registersGlobalBehaviorWhenRequestTypeNotResolvable() {
        PipelineBehavior<?, ?> behavior = new GenericBehavior() {
            @Override
            public String handle(IRequest<String> request, Next<String> next) {
                return "";
            }
        };

        postProcessor.postProcessAfterInitialization(behavior, "genericBehavior");

        verify(pipelineRegistry, never()).registerBehavior(any(), any());
        verify(pipelineRegistry).registerGlobalBehavior(behavior, null);
    }

    @Test
    void registersParameterizedBehaviorAsGlobalWithResolvedType() {
        PipelineBehavior<?, ?> behavior = new BehaviorWithoutGeneric();

        postProcessor.postProcessAfterInitialization(behavior, "noTypeBehavior");

        verify(pipelineRegistry, never()).registerBehavior(any(), any());
        verify(pipelineRegistry).registerGlobalBehavior(behavior, IRequest.class);
    }

    private static class SampleRequest implements IRequest<String> {
    }

    private static class SampleBehavior implements PipelineBehavior<SampleRequest, String> {
        @Override
        public String handle(SampleRequest request, Next<String> next) {
            return next.invoke();
        }
    }

    @Test
    void registersWildcardBehaviorAsGlobalWithResolvedType() {
        WildcardResponseBehavior behavior = new WildcardResponseBehavior();

        postProcessor.postProcessAfterInitialization(behavior, "wildcardBehavior");

        verify(pipelineRegistry, never()).registerBehavior(any(), any());
        verify(pipelineRegistry).registerGlobalBehavior(behavior, IRequest.class);
    }

    private abstract static class GenericBehavior implements PipelineBehavior<IRequest<String>, String> {
    }

    private static class BehaviorWithoutGeneric implements PipelineBehavior<IRequest<Object>, Object> {
        @Override
        public Object handle(IRequest<Object> request, Next<Object> next) {
            return next.invoke();
        }
    }

    private static class WildcardResponseBehavior
            implements PipelineBehavior<IRequest<Response<?>>, Response<?>> {
        @Override
        public Response<?> handle(IRequest<Response<?>> request, Next<Response<?>> next) {
            return next.invoke();
        }
    }
}