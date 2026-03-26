package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierSendAsyncTest {

    @Test
    void sendAsyncCommandReturnsSuccessResponse()
            throws ExecutionException, InterruptedException {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(
                TestCmd.class, new TestCmdHandler());

        CompletableFuture<Response<String>> future =
                fixture.courier().sendAsync(new TestCmd());

        Response<String> result = future.get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("async-ok", result.getData());
    }

    @Test
    void sendAsyncQueryReturnsSuccessResponse()
            throws ExecutionException, InterruptedException {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(
                TestQuery.class, new TestQueryHandler());

        CompletableFuture<Response<Integer>> future =
                fixture.courier().sendAsync(new TestQuery());

        Response<Integer> result = future.get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(42, result.getData());
    }

    @Test
    void sendAsyncHandlerErrorReturnsErrorResponse()
            throws ExecutionException, InterruptedException {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.handlerRegistry().registerHandler(
                TestCmd.class, new FailingCmdHandler());

        CompletableFuture<Response<String>> future =
                fixture.courier().sendAsync(new TestCmd());

        Response<String> result = future.get();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    void sendAsyncRejectsNull() {
        CourierTestFixture fixture = CourierTestFixture.create();
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> fixture.courier().sendAsync(null));
    }

    static class TestCmd implements ICommand<String> {
    }

    static class TestQuery implements IQuery<Integer> {
    }

    @SuppressWarnings("unused")
    static class TestCmdHandler {
        public String handle(TestCmd cmd) {
            return "async-ok";
        }
    }

    @SuppressWarnings("unused")
    static class TestQueryHandler {
        public Integer handle(TestQuery query) {
            return 42;
        }
    }

    @SuppressWarnings("unused")
    static class FailingCmdHandler {
        public String handle(TestCmd cmd) {
            throw new RuntimeException("async-boom");
        }
    }
}
