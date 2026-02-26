package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PipelineExecutorTypeDetectionTest {

    @Test
    void detectsCommandInterfacesIncludingSuperclasses() {
        PipelineExecutor executor = new PipelineExecutor(new PipelineRegistry());
        String category = invokeDetect(executor, new ChildCommand());
        assertEquals("Command", category);
    }

    @Test
    void detectsQueryInterface() {
        PipelineExecutor executor = new PipelineExecutor(new PipelineRegistry());
        String category = invokeDetect(executor, new SimpleQuery());
        assertEquals("Query", category);
    }

    @Test
    void detectsGenericRequestWhenNoCommandOrQuery() {
        PipelineExecutor executor = new PipelineExecutor(new PipelineRegistry());
        String category = invokeDetect(executor, new PlainRequest());
        assertEquals("Generic IRequest", category);
    }

    private String invokeDetect(PipelineExecutor executor, Object request) {
        try {
            var method = PipelineExecutor.class.getDeclaredMethod("detectRequestCategory", Object.class);
            method.setAccessible(true);
            return (String) method.invoke(executor, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class BaseCommand implements ICommand<String> { }
    private static class ChildCommand extends BaseCommand { }
    private static class SimpleQuery implements IQuery<String> { }
    private static class PlainRequest implements IRequest<String> { }
}

