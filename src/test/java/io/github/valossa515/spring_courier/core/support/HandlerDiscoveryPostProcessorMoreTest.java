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
}