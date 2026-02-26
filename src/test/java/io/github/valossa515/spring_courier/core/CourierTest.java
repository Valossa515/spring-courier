package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Objects;

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
        courier = new Courier(handlerRegistry, new io.github.valossa515.spring_courier.core.support.NotificationRegistry(), pipelineExecutor);
    }

    @Test
    void sendDelegatesToPipelineExecutorWithDiscoveredHandler() {
        SampleRequest request = new SampleRequest();
        SampleHandler handler = new SampleHandler();

        when(handlerRegistry.getHandler(SampleRequest.class)).thenReturn(handler);
        when(pipelineExecutor.execute(eq(request), any())).thenAnswer(invocation -> {
            PipelineExecutor.HandlerInvoker<SampleRequest, String> invoker = invocation.getArgument(1);
            // Retorna exatamente o que o executor real retornaria (Response<String>)
            return invoker.invoke();
        });

        var response = courier.send(request);

        assertEquals("handled", response.getData());
        verify(handlerRegistry).getHandler(SampleRequest.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<PipelineExecutor.HandlerInvoker<SampleRequest, String>> invokerCaptor =
                (ArgumentCaptor<PipelineExecutor.HandlerInvoker<SampleRequest, String>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(PipelineExecutor.HandlerInvoker.class);
        verify(pipelineExecutor).execute(eq(request), invokerCaptor.capture());
        assertNotNull(invokerCaptor.getValue());
    }

    @Test
    void sendInvokesHandlerDirectlyWhenUsingRealComponents() {
        CourierTestFixture fixture = CourierTestFixture.create();

        RealRequest request = new RealRequest();
        fixture.handlerRegistry().registerHandler(RealRequest.class, new RealHandler());

        var response = fixture.courier().send(request);

        assertEquals("real-response", response.getData());
    }

    @Test
    void sendReturnsErrorResponseWhenHandlerLacksHandleMethod() {
        CourierTestFixture fixture = CourierTestFixture.create();

        fixture.handlerRegistry().registerHandler(SampleRequest.class, new Object());

        var response = fixture.courier().send(new SampleRequest());

        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("No handle method"));
    }

    @Test
    void sendReturnsErrorResponseWhenHandlerThrowsException() {
        CourierTestFixture fixture = CourierTestFixture.create();

        fixture.handlerRegistry().registerHandler(RealRequest.class, new FailingHandler());

        var response = fixture.courier().send(new RealRequest());

        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("boom"));
    }

    private static class SampleRequest implements IRequest<String> {
    }

    @SuppressWarnings("unused")
    private static class SampleHandler {
        public String handle(SampleRequest request) {
            Objects.requireNonNull(request);
            return "handled";
        }
    }

    private static class RealRequest implements IRequest<String> {
    }

    @SuppressWarnings("unused")
    private static class RealHandler {
        public String handle(RealRequest request) {
            Objects.requireNonNull(request);
            return "real-response";
        }
    }

    @SuppressWarnings("unused")
    private static class FailingHandler {
        public String handle(RealRequest request) {
            Objects.requireNonNull(request);
            throw new IllegalStateException("boom");
        }
    }
}