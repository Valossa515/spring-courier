package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.PostProcessor;
import io.github.valossa515.spring_courier.core.pipelines.PreProcessor;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierPrePostProcessorTest {

    private Courier createCourier(ProcessorRegistry processorRegistry) {
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        handlerRegistry.registerHandler(TestCmd.class,
                new TestCmdHandler());
        NotificationRegistry notificationRegistry =
                new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        PipelineExecutor pipelineExecutor =
                new PipelineExecutor(pipelineRegistry);
        return new Courier(handlerRegistry, notificationRegistry,
                pipelineExecutor, processorRegistry, null, 30_000,
                NotificationPublishingStrategy.SEQUENTIAL);
    }

    @Test
    void preProcessorIsCalledBeforeHandler() {
        List<String> callOrder = new ArrayList<>();
        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        processorRegistry.registerPreProcessor(
                (PreProcessor<TestCmd>) request ->
                        callOrder.add("pre"));

        Courier courier = createCourier(processorRegistry);
        Response<String> result = courier.send(new TestCmd());

        assertTrue(result.isSuccess());
        assertTrue(callOrder.contains("pre"));
    }

    @Test
    void postProcessorIsCalledAfterHandler() {
        List<String> callOrder = new ArrayList<>();
        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        processorRegistry.registerPostProcessor(
                (PostProcessor<Object, Object>)
                        (request, response) ->
                                callOrder.add("post"));

        Courier courier = createCourier(processorRegistry);
        Response<String> result = courier.send(new TestCmd());

        assertTrue(result.isSuccess());
        assertTrue(callOrder.contains("post"));
    }

    @Test
    void preAndPostProcessorsExecuteInOrder() {
        List<String> callOrder = new ArrayList<>();
        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        processorRegistry.registerPreProcessor(
                (PreProcessor<TestCmd>) request ->
                        callOrder.add("pre"));
        processorRegistry.registerPostProcessor(
                (PostProcessor<Object, Object>)
                        (request, response) ->
                                callOrder.add("post"));

        Courier courier = createCourier(processorRegistry);
        courier.send(new TestCmd());

        assertEquals(List.of("pre", "post"), callOrder);
    }

    @Test
    void preProcessorExceptionDoesNotBlockExecution() {
        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        processorRegistry.registerPreProcessor(
                (PreProcessor<TestCmd>) request -> {
                    throw new RuntimeException("pre-boom");
                });

        Courier courier = createCourier(processorRegistry);
        Response<String> result = courier.send(new TestCmd());

        assertTrue(result.isSuccess());
        assertEquals("handled", result.getData());
    }

    @Test
    void postProcessorExceptionDoesNotBlockResponse() {
        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        processorRegistry.registerPostProcessor(
                (PostProcessor<Object, Object>)
                        (request, response) -> {
                            throw new RuntimeException("post-boom");
                        });

        Courier courier = createCourier(processorRegistry);
        Response<String> result = courier.send(new TestCmd());

        assertTrue(result.isSuccess());
    }

    @Test
    void exceptionHandlerInterceptsPipelineException() {
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        handlerRegistry.registerHandler(TestCmd.class,
                new TestCmdHandler());
        NotificationRegistry notificationRegistry =
                new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        pipelineRegistry.registerGlobalBehavior(
                new ThrowingBehavior(), TestCmd.class);
        PipelineExecutor pipelineExecutor =
                new PipelineExecutor(pipelineRegistry);

        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        processorRegistry.registerExceptionHandler(
                (IRequestExceptionHandler<Object, Object>)
                        (request, exception) ->
                                Response.error("intercepted", 422));

        Courier courier = new Courier(handlerRegistry,
                notificationRegistry, pipelineExecutor,
                processorRegistry, null, 30_000,
                NotificationPublishingStrategy.SEQUENTIAL);

        Response<String> result = courier.send(new TestCmd());
        assertNotNull(result);
        assertTrue(result.hasError());
        assertEquals(422, result.getStatusCode());
    }

    @Test
    void exceptionHandlerReturningNullFallsThrough() {
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        handlerRegistry.registerHandler(TestCmd.class,
                new TestCmdHandler());
        NotificationRegistry notificationRegistry =
                new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        pipelineRegistry.registerGlobalBehavior(
                new ThrowingBehavior(), TestCmd.class);
        PipelineExecutor pipelineExecutor =
                new PipelineExecutor(pipelineRegistry);

        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        processorRegistry.registerExceptionHandler(
                (IRequestExceptionHandler<Object, Object>)
                        (request, exception) -> null);

        Courier courier = new Courier(handlerRegistry,
                notificationRegistry, pipelineExecutor,
                processorRegistry, null, 30_000,
                NotificationPublishingStrategy.SEQUENTIAL);

        // No handler matched — exception re-thrown
        assertThrows(RuntimeException.class,
                () -> courier.send(new TestCmd()));
    }

    static class TestCmd implements ICommand<String> {
    }

    @SuppressWarnings("unused")
    static class TestCmdHandler {
        public String handle(TestCmd cmd) {
            return "handled";
        }
    }

    @SuppressWarnings("unused")
    static class FailingCmdHandler {
        public String handle(TestCmd cmd) {
            throw new RuntimeException("handler-boom");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static class ThrowingBehavior
            implements PipelineBehavior {
        @Override
        public Object handle(IRequest request,
                PipelineBehavior.Next next) {
            throw new RuntimeException("pipeline-boom");
        }
    }
}
