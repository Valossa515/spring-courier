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
    void behaviorRegisteredUnderKeyIsIncludedWithoutTypeReExtraction() {
        IncompatibleBehavior wrongKeyBehavior = new IncompatibleBehavior();
        CompatibleBehavior compatibleBehavior = new CompatibleBehavior();

        // Registering under TestRequest key — even if the behavior's generics
        // say AnotherRequest, the key takes precedence (proxy-safe).
        pipelineRegistry.registerBehavior(TestRequest.class, wrongKeyBehavior);
        pipelineRegistry.registerBehavior(TestRequest.class, compatibleBehavior);

        List<PipelineBehavior<TestRequest, String>> behaviors = pipelineRegistry.getBehaviors(TestRequest.class);

        assertEquals(2, behaviors.size(), "Both behaviors under the same key should be returned");
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
    void hasBehaviorsForReturnsTrueWhenGlobalBehaviorsMatchRequestType() {
        pipelineRegistry.registerGlobalBehavior(new DefaultBehavior());

        assertTrue(pipelineRegistry.hasBehaviorsFor(TestRequest.class));
        // DefaultBehavior targets TestRequest, not AnotherRequest
        assertFalse(pipelineRegistry.hasBehaviorsFor(AnotherRequest.class));
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

    @Test
    void incompatibleGlobalBehaviorsAreFilteredOut() {
        IncompatibleBehavior incompatibleGlobal = new IncompatibleBehavior();
        pipelineRegistry.registerGlobalBehavior(incompatibleGlobal);

        List<PipelineBehavior<TestRequest, String>> behaviors = pipelineRegistry.getBehaviors(TestRequest.class);

        assertTrue(behaviors.isEmpty(), "Incompatible global behavior should not appear for unrelated request");
    }

    @Test
    void compatibleGlobalBehaviorsAreIncludedIncompatibleAreExcluded() {
        DefaultBehavior compatibleGlobal = new DefaultBehavior();
        IncompatibleBehavior incompatibleGlobal = new IncompatibleBehavior();

        pipelineRegistry.registerGlobalBehavior(compatibleGlobal);
        pipelineRegistry.registerGlobalBehavior(incompatibleGlobal);

        List<PipelineBehavior<TestRequest, String>> behaviors = pipelineRegistry.getBehaviors(TestRequest.class);

        assertEquals(1, behaviors.size(), "Only compatible global behavior should be returned");
        assertSame(compatibleGlobal, behaviors.get(0));
    }

    @Test
    void incompatibleGlobalBehaviorStillAppearsForMatchingRequestType() {
        IncompatibleBehavior globalForAnother = new IncompatibleBehavior();
        pipelineRegistry.registerGlobalBehavior(globalForAnother);

        List<PipelineBehavior<AnotherRequest, String>> behaviors = pipelineRegistry.getBehaviors(AnotherRequest.class);

        assertEquals(1, behaviors.size(), "Global behavior should appear for its own compatible request type");
        assertSame(globalForAnother, behaviors.get(0));
    }

    @Test
    void typeErasedGlobalBehaviorIsExcludedFromAllRequests() {
        // Simulates a @Bean factory method where generic type info is erased:
        // registerGlobalBehavior(behavior, null) means type could not be resolved.
        @SuppressWarnings("rawtypes")
        PipelineBehavior erasedBehavior = new PipelineBehavior() {
            @Override
            public Object handle(IRequest request, Next next) {
                return next.invoke();
            }
        };
        @SuppressWarnings("unchecked")
        PipelineBehavior<IRequest<Object>, Object> typed = erasedBehavior;
        pipelineRegistry.registerGlobalBehavior(typed, null);

        List<PipelineBehavior<TestRequest, String>> behaviors =
                pipelineRegistry.getBehaviors(TestRequest.class);

        assertTrue(behaviors.isEmpty(),
                "Type-erased global behavior (null request type) must not match any request");
    }

    @Test
    void globalBehaviorWithExplicitResolvedTypeIsIncluded() {
        DefaultBehavior behavior = new DefaultBehavior();
        // Simulates BehaviorDiscoveryPostProcessor passing the resolved type
        pipelineRegistry.registerGlobalBehavior(behavior, TestRequest.class);

        List<PipelineBehavior<TestRequest, String>> behaviors =
                pipelineRegistry.getBehaviors(TestRequest.class);

        assertEquals(1, behaviors.size());
        assertSame(behavior, behaviors.get(0));
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
