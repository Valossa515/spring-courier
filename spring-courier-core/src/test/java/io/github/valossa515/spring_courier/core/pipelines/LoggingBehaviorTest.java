package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingBehaviorTest {

    @Test
    void hasHighestPrecedenceOrder() {
        LoggingBehavior<?, ?> behavior = new LoggingBehavior<>();
        assertEquals(Ordered.HIGHEST_PRECEDENCE, behavior.getOrder());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void delegatesToNextAndReturnsResult() {
        LoggingBehavior behavior = new LoggingBehavior<>();
        Response<String> expected = Response.success("ok");

        Object result = behavior.handle(
                new TestCmd(), () -> expected);

        assertEquals(expected, result);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void delegatesToNextWithRawResult() {
        LoggingBehavior behavior = new LoggingBehavior<>();

        Object result = behavior.handle(
                new TestRawCmd(), () -> "raw");

        assertEquals("raw", result);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void logsErrorResponseButStillReturns() {
        LoggingBehavior behavior = new LoggingBehavior<>();
        Response<String> error = Response.error("oops", 500);

        Object result = behavior.handle(new TestCmd(), () -> error);

        assertNotNull(result);
        assertTrue(((Response<?>) result).hasError());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void propagatesExceptionFromNext() {
        LoggingBehavior behavior = new LoggingBehavior<>();

        assertThrows(RuntimeException.class,
                () -> behavior.handle(new TestCmd(), () -> {
                    throw new RuntimeException("boom");
                }));
    }

    static class TestCmd implements ICommand<Response<String>> {
    }

    static class TestRawCmd implements ICommand<String> {
    }
}
