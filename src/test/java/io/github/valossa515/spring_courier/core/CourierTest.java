package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CourierTest {

    private HandlerRegistry handlerRegistry;
    private PipelineExecutor pipelineExecutor;
    private Courier courier;

    @BeforeEach
    void setUp() {
        handlerRegistry = mock(HandlerRegistry.class);
        pipelineExecutor = mock(PipelineExecutor.class);
        courier = new Courier(handlerRegistry, pipelineExecutor);
    }

    @Test
    void sendDelegatesToPipelineExecutorWithDiscoveredHandler() {
        SampleRequest request = new SampleRequest();
        SampleHandler handler = new SampleHandler();

        when(handlerRegistry.getHandler(SampleRequest.class)).thenReturn(handler);
        when(pipelineExecutor.execute(eq(request), any())).thenAnswer(invocation -> {
            PipelineExecutor.HandlerInvoker<SampleRequest, String> invoker = invocation.getArgument(1);
            return invoker.invoke();
        });

        String result = courier.send(request);

        assertEquals("handled", result);
        verify(handlerRegistry).getHandler(SampleRequest.class);
        ArgumentCaptor<PipelineExecutor.HandlerInvoker<SampleRequest, String>> invokerCaptor =
                ArgumentCaptor.forClass(PipelineExecutor.HandlerInvoker.class);
        verify(pipelineExecutor).execute(eq(request), invokerCaptor.capture());
        assertNotNull(invokerCaptor.getValue());
    }

    @Test
    void sendInvokesHandlerDirectlyWhenUsingRealComponents() {
        HandlerRegistry realRegistry = new HandlerRegistry();
        PipelineExecutor realExecutor = new PipelineExecutor(new PipelineRegistry());
        Courier realCourier = new Courier(realRegistry, realExecutor);

        RealRequest request = new RealRequest();
        realRegistry.registerHandler(RealRequest.class, new RealHandler());

        String result = realCourier.send(request);

        assertEquals("real-response", result);
    }

    @Test
    void sendThrowsWhenHandlerLacksHandleMethod() {
        HandlerRegistry realRegistry = new HandlerRegistry();
        PipelineExecutor realExecutor = new PipelineExecutor(new PipelineRegistry());
        Courier realCourier = new Courier(realRegistry, realExecutor);

        realRegistry.registerHandler(SampleRequest.class, new Object());

        SampleRequest request = new SampleRequest();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> realCourier.send(request));
        assertTrue(exception.getMessage().contains("No handle method"));
    }

    @Test
    void sendWrapsExceptionsThrownByHandler() {
        HandlerRegistry realRegistry = new HandlerRegistry();
        PipelineExecutor realExecutor = new PipelineExecutor(new PipelineRegistry());
        Courier realCourier = new Courier(realRegistry, realExecutor);

        realRegistry.registerHandler(RealRequest.class, new FailingHandler());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> realCourier.send(new RealRequest()));
        assertTrue(exception.getMessage().contains("Handler execution failed"));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    private static class SampleRequest implements IRequest<String> {
    }

    private static class SampleHandler {
        public String handle(SampleRequest request) {
            return "handled";
        }
    }

    private static class RealRequest implements IRequest<String> {
    }

    private static class RealHandler {
        public String handle(RealRequest request) {
            return "real-response";
        }
    }

    private static class FailingHandler {
        public String handle(RealRequest request) {
            throw new IllegalStateException("boom");
        }
    }
}
