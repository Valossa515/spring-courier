package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    static class TestNotification implements INotification {}

    static class TestNotificationHandler implements NotificationHandler<TestNotification> {
        @Override
        public void handle(TestNotification notification) {
            // no-op
        }
    }
}
