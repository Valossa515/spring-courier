package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class NotificationDiscoveryPostProcessorAdditionalTest {
    @Test
    void registersNotificationHandler() {
        NotificationRegistry registry = mock(NotificationRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        NotificationDiscoveryPostProcessor pp = new NotificationDiscoveryPostProcessor(registry, ctx);

        TestNotificationHandler bean = new TestNotificationHandler();
        pp.postProcessAfterInitialization(bean, "notifHandler");

        ArgumentCaptor<Class<?>> notifCaptor = ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<Object> handlerCaptor = ArgumentCaptor.forClass(Object.class);
        verify(registry).registerHandler(notifCaptor.capture(), handlerCaptor.capture());
        assertSame(TestNotification.class, notifCaptor.getValue());
        assertSame(bean, handlerCaptor.getValue());
    }

    @Test
    void skipsRegistrationWhenGenericTypeNotResolvable() {
        NotificationRegistry registry = mock(NotificationRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        NotificationDiscoveryPostProcessor pp = new NotificationDiscoveryPostProcessor(registry, ctx);

        NoGenericHandler bean = new NoGenericHandler();
        pp.postProcessAfterInitialization(bean, "noGeneric");

        verify(registry, never()).registerHandler(any(), any());
    }

    @Test
    void skipsRegistrationWhenGenericTypeIsTypeVariable() {
        NotificationRegistry registry = mock(NotificationRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        NotificationDiscoveryPostProcessor pp = new NotificationDiscoveryPostProcessor(registry, ctx);

        GenericNotificationHandler<?> bean = new GenericNotificationHandler<>();
        pp.postProcessAfterInitialization(bean, "genericHandler");

        verify(registry, never()).registerHandler(any(), any());
    }

    @Test
    void handlesRegistrationErrorsGracefully() {
        NotificationRegistry registry = mock(NotificationRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        NotificationDiscoveryPostProcessor pp = new NotificationDiscoveryPostProcessor(registry, ctx);

        TestNotificationHandler bean = new TestNotificationHandler();
        doThrow(new RuntimeException("boom"))
                .when(registry)
                .registerHandler(TestNotification.class, bean);

        pp.postProcessAfterInitialization(bean, "explodingHandler");

        verify(registry).registerHandler(TestNotification.class, bean);
    }

    static class TestNotification implements INotification {}

    static class TestNotificationHandler implements NotificationHandler<TestNotification> {
        @Override
        public void handle(TestNotification notification) {
            // no-op
        }
    }

    static class NoGenericHandler implements NotificationHandler<INotification> {
        @Override
        public void handle(INotification notification) {
            // no-op
        }
    }

    static class GenericNotificationHandler<T extends INotification> implements NotificationHandler<T> {
        @Override
        public void handle(T notification) {
            // no-op
        }
    }
}
