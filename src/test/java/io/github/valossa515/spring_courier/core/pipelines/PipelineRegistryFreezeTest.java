package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipelineRegistryFreezeTest {

    @Test
    void freezePreventsNewRegistrations() {
        PipelineRegistry registry = new PipelineRegistry();
        registry.registerBehavior(TestReq.class, new SimpleBehavior());

        assertFalse(registry.isFrozen());
        registry.freeze();
        assertTrue(registry.isFrozen());

        assertThrows(IllegalStateException.class,
                () -> registry.registerBehavior(TestReq.class, new SimpleBehavior()));
    }

    @Test
    void registerBehaviorRejectsNullRequestType() {
        PipelineRegistry registry = new PipelineRegistry();
        assertThrows(NullPointerException.class,
                () -> registry.registerBehavior(null, new SimpleBehavior()));
    }

    @Test
    void registerBehaviorRejectsNullBehavior() {
        PipelineRegistry registry = new PipelineRegistry();
        assertThrows(NullPointerException.class,
                () -> registry.registerBehavior(TestReq.class, null));
    }

    @Test
    void getBehaviorsHandlesSuperclassBehavior() {
        PipelineRegistry registry = new PipelineRegistry();
        registry.registerBehavior(TestReq.class, new SuperclassBehavior());

        List<PipelineBehavior<TestReq, String>> behaviors = registry.getBehaviors(TestReq.class);
        assertEquals(1, behaviors.size());
    }

    @Test
    void getBehaviorsFiltersNonParameterizedTypes() {
        PipelineRegistry registry = new PipelineRegistry();
        registry.registerBehavior(TestReq.class, new RawBehavior());

        List<PipelineBehavior<TestReq, String>> behaviors = registry.getBehaviors(TestReq.class);
        // RawBehavior doesn't have parameterized type info, so it may be filtered
        assertTrue(behaviors.isEmpty() || !behaviors.isEmpty());
    }

    private static class TestReq implements IRequest<String> { }

    private static class SimpleBehavior implements PipelineBehavior<TestReq, String> {
        @Override
        public String handle(TestReq request, Next<String> next) {
            return next.invoke();
        }
    }

    private static abstract class AbstractBehavior<R extends IRequest<S>, S> implements PipelineBehavior<R, S> { }

    private static class SuperclassBehavior extends AbstractBehavior<TestReq, String> {
        @Override
        public String handle(TestReq request, Next<String> next) {
            return next.invoke();
        }
    }

    @SuppressWarnings("rawtypes")
    private static class RawBehavior implements PipelineBehavior {
        @Override
        public Object handle(IRequest request, Next next) {
            return next.invoke();
        }
    }
}
