package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PipelineExecutorTest {

    private PipelineRegistry pipelineRegistry;
    private PipelineExecutor pipelineExecutor;

    @BeforeEach
    void setUp() {
        pipelineRegistry = new PipelineRegistry();
        pipelineExecutor = new PipelineExecutor(pipelineRegistry);
    }

    @Test
    void executeInvokesHandlerDirectlyWhenNoBehaviors() {
        TestRequest request = new TestRequest();
        AtomicInteger handlerInvocations = new AtomicInteger();

        var response = pipelineExecutor.execute(request, () -> {
            handlerInvocations.incrementAndGet();
            return "handler";
        });

        assertEquals("handler", response.getData());
        assertEquals(1, handlerInvocations.get());
    }

    @Test
    void executeTraversesBehaviorChainInOrder() {
        List<String> executionOrder = new ArrayList<>();
        RecordingBehavior first = new RecordingBehavior("first", executionOrder);
        RecordingBehavior second = new RecordingBehavior("second", executionOrder);
        RecordingBehavior third = new RecordingBehavior("third", executionOrder);

        pipelineRegistry.registerBehavior(TestRequest.class, first);
        pipelineRegistry.registerBehavior(TestRequest.class, second);
        pipelineRegistry.registerBehavior(TestRequest.class, third);

        TestRequest request = new TestRequest();

        var response = pipelineExecutor.execute(request, () -> {
            executionOrder.add("handler");
            return "result";
        });

        assertEquals("result -> third -> second -> first", response.getData());
        assertEquals(List.of("first", "second", "third", "handler"), executionOrder);
    }

    // ✅ O request define apenas o tipo final (String), o executor normaliza pra Response<String>
    private static class TestRequest implements IRequest<String> { }

    // ✅ O behavior agora trabalha com o tipo de retorno "puro", e não Response<>
    private record RecordingBehavior(String name, List<String> executionOrder)
            implements PipelineBehavior<TestRequest, String> {

        @Override
        public String handle(TestRequest request, Next<String> next) {
            executionOrder.add(name);
            return Response.success(next.invoke()).getData() + " -> " + name;
        }
    }
}