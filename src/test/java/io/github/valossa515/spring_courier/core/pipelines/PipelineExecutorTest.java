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

    private static class TestRequest implements IRequest<Response<String>> { }

    private static class RecordingBehavior implements PipelineBehavior<TestRequest, Response<String>> {
        private final String name;
        private final List<String> executionOrder;

        private RecordingBehavior(String name, List<String> executionOrder) {
            this.name = name;
            this.executionOrder = executionOrder;
        }

        @Override
        public Response<String> handle(TestRequest request, Next<Response<String>> next) {
            executionOrder.add(name);
            var nextResponse = next.invoke();
            String chained = nextResponse.getData() + " -> " + name;
            return Response.success(chained);
        }
    }
}
