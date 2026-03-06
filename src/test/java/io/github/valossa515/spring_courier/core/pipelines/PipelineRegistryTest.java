package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipelineRegistryTest {

    private PipelineRegistry pipelineRegistry;

    @BeforeEach
    void setUp() {
        pipelineRegistry = new PipelineRegistry();
    }

    @Test
    void behaviorsAreStoredOrderedByPriority() {
        OrderedBehavior orderedBehavior = new OrderedBehavior();
        HighOrderAnnotationBehavior annotationBehavior = new HighOrderAnnotationBehavior();
        DefaultBehavior defaultBehavior = new DefaultBehavior();

        pipelineRegistry.registerBehavior(TestRequest.class, defaultBehavior);
        pipelineRegistry.registerBehavior(TestRequest.class, annotationBehavior);
        pipelineRegistry.registerBehavior(TestRequest.class, orderedBehavior);

        List<PipelineBehavior<TestRequest, String>> behaviors = pipelineRegistry.getBehaviors(TestRequest.class);

        assertEquals(3, behaviors.size());
        assertSame(orderedBehavior, behaviors.get(0), "Behavior implementing Ordered should run first");
        assertSame(annotationBehavior, behaviors.get(1), "Behavior annotated with lower order value should run second");
        assertSame(defaultBehavior, behaviors.get(2), "Behavior without order metadata should run last");
    }

    @Test
    void incompatibleBehaviorsAreFilteredOut() {
        IncompatibleBehavior incompatibleBehavior = new IncompatibleBehavior();
        CompatibleBehavior compatibleBehavior = new CompatibleBehavior();

        pipelineRegistry.registerBehavior(TestRequest.class, incompatibleBehavior);
        pipelineRegistry.registerBehavior(TestRequest.class, compatibleBehavior);

        List<PipelineBehavior<TestRequest, String>> behaviors = pipelineRegistry.getBehaviors(TestRequest.class);

        assertEquals(1, behaviors.size());
        assertSame(compatibleBehavior, behaviors.get(0));
    }

    @Test
    void registryMetadataQueriesReflectRegisteredBehaviors() {
        pipelineRegistry.registerBehavior(TestRequest.class, new DefaultBehavior());
        pipelineRegistry.registerBehavior(TestRequest.class, new DefaultBehavior());

        assertTrue(pipelineRegistry.hasBehaviorsFor(TestRequest.class));
        assertEquals(2, pipelineRegistry.getBehaviorCount());
    }

    @Test
    void hasBehaviorsForReturnsFalseWhenNone() {
        assertFalse(pipelineRegistry.hasBehaviorsFor(TestRequest.class));
    }

    @Test
    void globalBehaviorsAreIncludedInGetBehaviors() {
        DefaultBehavior globalBehavior = new DefaultBehavior();
        pipelineRegistry.registerGlobalBehavior(globalBehavior);

        List<PipelineBehavior<TestRequest, String>> behaviors = pipelineRegistry.getBehaviors(TestRequest.class);

        assertEquals(1, behaviors.size());
        assertSame(globalBehavior, behaviors.get(0));
    }

    @Test
    void globalBehaviorsAreMergedWithSpecificBehaviors() {
        DefaultBehavior globalBehavior = new DefaultBehavior();
        CompatibleBehavior specificBehavior = new CompatibleBehavior();

        pipelineRegistry.registerGlobalBehavior(globalBehavior);
        pipelineRegistry.registerBehavior(TestRequest.class, specificBehavior);

        List<PipelineBehavior<TestRequest, String>> behaviors = pipelineRegistry.getBehaviors(TestRequest.class);

        assertEquals(2, behaviors.size());
    }

    @Test
    void hasBehaviorsForReturnsTrueWhenGlobalBehaviorsExist() {
        pipelineRegistry.registerGlobalBehavior(new DefaultBehavior());

        assertTrue(pipelineRegistry.hasBehaviorsFor(TestRequest.class));
        assertTrue(pipelineRegistry.hasBehaviorsFor(AnotherRequest.class));
    }

    @Test
    void getBehaviorCountIncludesGlobalBehaviors() {
        pipelineRegistry.registerGlobalBehavior(new DefaultBehavior());
        pipelineRegistry.registerBehavior(TestRequest.class, new DefaultBehavior());

        assertEquals(2, pipelineRegistry.getBehaviorCount());
    }

    @Test
    void registerGlobalBehaviorRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> pipelineRegistry.registerGlobalBehavior(null));
    }

    private static class TestRequest implements IRequest<String> {
    }

    private static class AnotherRequest implements IRequest<String> {
    }

    private static class DefaultBehavior implements PipelineBehavior<TestRequest, String> {
        @Override
        public String handle(TestRequest request, Next<String> next) {
            return next.invoke();
        }
    }

    private static class OrderedBehavior implements PipelineBehavior<TestRequest, String>, Ordered {
        @Override
        public int getOrder() {
            return -10;
        }

        @Override
        public String handle(TestRequest request, Next<String> next) {
            return next.invoke();
        }
    }

    @Order(5)
    private static class HighOrderAnnotationBehavior implements PipelineBehavior<TestRequest, String> {
        @Override
        public String handle(TestRequest request, Next<String> next) {
            return next.invoke();
        }
    }

    private static class CompatibleBehavior implements PipelineBehavior<TestRequest, String> {
        @Override
        public String handle(TestRequest request, Next<String> next) {
            return next.invoke();
        }
    }

    private static class IncompatibleBehavior implements PipelineBehavior<AnotherRequest, String> {
        @Override
        public String handle(AnotherRequest request, Next<String> next) {
            return next.invoke();
        }
    }
}
