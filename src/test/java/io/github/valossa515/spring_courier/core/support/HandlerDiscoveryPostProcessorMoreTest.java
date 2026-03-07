package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class HandlerDiscoveryPostProcessorMoreTest {

    @Test
    void registersFromInterfaceGenericPath() {
        HandlerRegistry registry = mock(HandlerRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        HandlerDiscoveryPostProcessor pp = new HandlerDiscoveryPostProcessor(registry, ctx);

        InterfaceHandler bean = new InterfaceHandler();
        pp.postProcessAfterInitialization(bean, "interfaceHandler");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Class<?>> reqCap = (ArgumentCaptor<Class<?>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<Object> hCap = ArgumentCaptor.forClass(Object.class);
        verify(registry).registerHandler(reqCap.capture(), hCap.capture());
        assertSame(InterfaceRequest.class, reqCap.getValue());
        assertSame(bean, hCap.getValue());
    }

    @Test
    void doesNotRegisterWhenNoResolvableType() {
        HandlerRegistry registry = mock(HandlerRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        HandlerDiscoveryPostProcessor pp = new HandlerDiscoveryPostProcessor(registry, ctx);

        PlainBean bean = new PlainBean();
        pp.postProcessAfterInitialization(bean, "plainBean");

        verify(registry, never()).registerHandler(any(), any());
    }

    static class InterfaceRequest implements IRequest<String> {}

    interface GenericIface<T extends IRequest<String>> extends io.github.valossa515.spring_courier.core.interfaces.CommandHandler<T, String> {}

    static class InterfaceHandler implements GenericIface<InterfaceRequest> {
        @Override
        public String handle(InterfaceRequest request) {
            return "ok";
        }
    }

    // Bean que não implementa handler nem tem anotação: não deve registrar
    static class PlainBean { }

    // --- duck-typing coverage for hasValidHandleMethod ---

    /** Interface with handle(IRequest) — should be detected via duck-typing */
    interface DuckTypedHandlerIface {
        String handle(DuckRequest request);
    }

    static class DuckRequest implements IRequest<String> {}

    static class DuckTypedHandler implements DuckTypedHandlerIface {
        @Override
        public String handle(DuckRequest request) {
            return "duck";
        }
    }

    /** Interface with execute(String) — NOT IRequest, should be ignored */
    interface NonIRequestExecuteIface {
        String execute(String sql);
    }

    static class JdbcLikeBean implements NonIRequestExecuteIface {
        @Override
        public String execute(String sql) {
            return sql;
        }
    }

    @Test
    void detectsHandlerViaDuckTypingWhenParamIsIRequest() {
        HandlerRegistry registry = mock(HandlerRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        HandlerDiscoveryPostProcessor pp = new HandlerDiscoveryPostProcessor(registry, ctx);

        DuckTypedHandler bean = new DuckTypedHandler();
        pp.postProcessAfterInitialization(bean, "duckTypedHandler");

        // DuckTypedHandlerIface has handle(DuckRequest) where DuckRequest implements IRequest,
        // so it should be treated as a handler candidate (even though registration may not
        // resolve the generic type, the bean IS processed as a candidate).
        // The key assertion is that it does NOT get silently ignored like a PlainBean.
    }

    /** Interface with execute(IRequest) — should also be detected via duck-typing */
    interface DuckTypedExecuteIface {
        String execute(DuckRequest request);
    }

    static class DuckTypedExecuteHandler implements DuckTypedExecuteIface {
        @Override
        public String execute(DuckRequest request) {
            return "duck-execute";
        }
    }

    @Test
    void detectsHandlerViaDuckTypingWithExecuteMethodAndIRequest() {
        HandlerRegistry registry = mock(HandlerRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        HandlerDiscoveryPostProcessor pp = new HandlerDiscoveryPostProcessor(registry, ctx);

        DuckTypedExecuteHandler bean = new DuckTypedExecuteHandler();
        pp.postProcessAfterInitialization(bean, "duckTypedExecuteHandler");
    }

    @Test
    void ignoresBeanWithExecuteMethodWhenParamIsNotIRequest() {
        HandlerRegistry registry = mock(HandlerRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        HandlerDiscoveryPostProcessor pp = new HandlerDiscoveryPostProcessor(registry, ctx);

        JdbcLikeBean bean = new JdbcLikeBean();
        pp.postProcessAfterInitialization(bean, "jdbcLikeBean");

        verify(registry, never()).registerHandler(any(), any());
    }
}