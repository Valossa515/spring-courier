package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.annotations.ExposeHandler;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HandlerDiscoveryPostProcessorTest {

    private HandlerRegistry handlerRegistry;
    private HandlerDiscoveryPostProcessor postProcessor;

    @BeforeEach
    void setUp() {
        handlerRegistry = mock(HandlerRegistry.class);
        postProcessor = new HandlerDiscoveryPostProcessor(handlerRegistry, mock(ApplicationContext.class));
    }

    @Test
    void registersCommandHandlerImplementations() {
        SampleCommandHandler bean = new SampleCommandHandler();

        postProcessor.postProcessAfterInitialization(bean, "sampleCommandHandler");

        verify(handlerRegistry).registerHandler(SampleCommand.class, bean);
    }

    @Test
    void registersAnnotatedQueryHandlers() {
        AnnotatedQueryHandler bean = new AnnotatedQueryHandler();

        postProcessor.postProcessAfterInitialization(bean, "annotatedQueryHandler");

        verify(handlerRegistry).registerHandler(SampleQuery.class, bean);
    }

    @Test
    void ignoresBeansWithoutHandlerContract() {
        Object bean = new Object();

        postProcessor.postProcessAfterInitialization(bean, "regularBean");

        verifyNoInteractions(handlerRegistry);
    }

    @Test
    void cachesHandlerInterfaceLookups() throws Exception {
        SampleCommandHandler bean = new SampleCommandHandler();

        postProcessor.postProcessAfterInitialization(bean, "first");
        postProcessor.postProcessAfterInitialization(bean, "second");

        Field cacheField = HandlerDiscoveryPostProcessor.class.getDeclaredField("handlerInterfaceCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Class<?>, Boolean> cache = (Map<Class<?>, Boolean>) cacheField.get(postProcessor);

        assertTrue(cache.containsKey(CommandHandler.class));
        assertEquals(1, cache.size());
    }

    private static class SampleCommand implements IRequest<String> {
    }

    private static class SampleQuery implements IRequest<String> {
    }

    private static class SampleCommandHandler implements CommandHandler<SampleCommand, String> {
        @Override
        public String handle(SampleCommand command) {
            return "ok";
        }
    }

    @ExposeHandler
    private static class AnnotatedQueryHandler implements QueryHandler<SampleQuery, String> {
        @Override
        public String handle(SampleQuery query) {
            return "ok";
        }
    }
}
