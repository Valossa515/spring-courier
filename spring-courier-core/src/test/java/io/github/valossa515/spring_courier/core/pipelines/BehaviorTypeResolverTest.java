package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BehaviorTypeResolverTest {

    @Test
    void resolvesConcretRequestType() {
        Class<?> result = BehaviorTypeResolver.extractRequestType(ConcreteBehavior.class);

        assertEquals(ConcreteRequest.class, result);
    }

    @Test
    void resolvesParameterizedRequestType() {
        Class<?> result = BehaviorTypeResolver.extractRequestType(WildcardBehavior.class);

        assertEquals(IRequest.class, result);
    }

    @Test
    void resolvesParameterizedResponseWildcard() {
        Class<?> result = BehaviorTypeResolver.extractRequestType(ResponseWildcardBehavior.class);

        assertEquals(IRequest.class, result);
    }

    @Test
    void returnsNullForFullyErasedTypeVariable() {
        @SuppressWarnings("rawtypes")
        Class<?> result = BehaviorTypeResolver.extractRequestType(RawBehavior.class);

        assertNull(result);
    }

    @Test
    void returnsNullForNonBehaviorClass() {
        Class<?> result = BehaviorTypeResolver.extractRequestType(String.class);

        assertNull(result);
    }

    @Test
    void returnsNullWhenGenericInfoLostInSuperclass() {
        Class<?> result = BehaviorTypeResolver.extractRequestType(SubclassBehavior.class);

        assertNull(result,
                "SubclassBehavior extends BaseBehavior without re-declaring generics; "
                        + "type info is on BaseBehavior's interface, not accessible from subclass");
    }

    // --- test fixtures ---

    private static class ConcreteRequest implements IRequest<String> {
    }

    private static class ConcreteBehavior implements PipelineBehavior<ConcreteRequest, String> {
        @Override
        public String handle(ConcreteRequest request, Next<String> next) {
            return next.invoke();
        }
    }

    private static class WildcardBehavior
            implements PipelineBehavior<IRequest<String>, String> {
        @Override
        public String handle(IRequest<String> request, Next<String> next) {
            return next.invoke();
        }
    }

    private static class ResponseWildcardBehavior
            implements PipelineBehavior<IRequest<Response<?>>, Response<?>> {
        @Override
        public Response<?> handle(IRequest<Response<?>> request, Next<Response<?>> next) {
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

    private abstract static class BaseBehavior
            implements PipelineBehavior<ConcreteRequest, String> {
    }

    private static class SubclassBehavior extends BaseBehavior {
        @Override
        public String handle(ConcreteRequest request, Next<String> next) {
            return next.invoke();
        }
    }
}
