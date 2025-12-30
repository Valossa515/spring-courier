package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.annotations.ExposeHandler;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.security.Permission;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HandlerDiscoveryPostProcessorAdditionalTest {
    @Test
    void registersAnnotatedCommandHandler() {
        HandlerRegistry registry = mock(HandlerRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        HandlerDiscoveryPostProcessor pp = new HandlerDiscoveryPostProcessor(registry, ctx);

        AnnotatedHandler bean = new AnnotatedHandler();
        pp.postProcessAfterInitialization(bean, "annotatedHandler");

        ArgumentCaptor<Class> reqCaptor = ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<Object> handlerCaptor = ArgumentCaptor.forClass(Object.class);
        verify(registry).registerHandler(reqCaptor.capture(), handlerCaptor.capture());
        assertSame(SampleRequest.class, reqCaptor.getValue());
        assertSame(bean, handlerCaptor.getValue());
    }

    @Test
    void registersViaGenericSuperclass() {
        HandlerRegistry registry = mock(HandlerRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        HandlerDiscoveryPostProcessor pp = new HandlerDiscoveryPostProcessor(registry, ctx);

        DerivedHandler bean = new DerivedHandler();
        pp.postProcessAfterInitialization(bean, "derivedHandler");

        verify(registry).registerHandler(SampleRequest.class, bean);
    }

    @Test
    @SuppressWarnings("removal")
    void handlesSecurityExceptionWhenInspectingMethods() throws Exception {
        HandlerRegistry registry = mock(HandlerRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        HandlerDiscoveryPostProcessor pp = new HandlerDiscoveryPostProcessor(registry, ctx);

        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof RuntimePermission
                        && "accessDeclaredMembers".equals(perm.getName())) {
                    throw new SecurityException("denied");
                }
            }
        });

        try {
            Method method = HandlerDiscoveryPostProcessor.class
                    .getDeclaredMethod("hasValidHandleMethod", Class.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(pp, SensitiveInterface.class);
            assertFalse(result);
        } finally {
            System.setSecurityManager(original);
        }
    }

    static class SampleRequest implements IRequest<String> {}

    @ExposeHandler
    static class AnnotatedHandler implements CommandHandler<SampleRequest, String> {
        @Override
        public String handle(SampleRequest request) {
            return "ok";
        }
    }

    abstract static class BaseHandler<T extends IRequest<String>> implements CommandHandler<T,String> {}

    static class DerivedHandler extends BaseHandler<SampleRequest> {
        @Override
        public String handle(SampleRequest request) {
            return "derived";
        }
    }

    interface SensitiveInterface {
        void handle(SampleRequest request);
    }
}
