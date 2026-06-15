package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.*;

class NotificationDiscoveryPostProcessorMoreTest {
    @Test
    void doesNotRegisterWhenGenericTypeNotResolved() {
        NotificationRegistry registry = mock(NotificationRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        NotificationDiscoveryPostProcessor pp = new NotificationDiscoveryPostProcessor(registry, ctx);

        RawHandler bean = new RawHandler();
        pp.postProcessAfterInitialization(bean, "rawHandler");

        verify(registry, never()).registerHandler(any(), any());
    }

    static class DummyNotification implements INotification {}

    // Implementa NotificationHandler sem generics concretos para cair no branch não resolvido
    static class RawHandler implements NotificationHandler {
        @Override
        public void handle(INotification notification) { /* no-op */ }
    }
}
