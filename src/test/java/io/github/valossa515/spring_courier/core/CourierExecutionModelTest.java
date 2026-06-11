package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.annotations.Timeout;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.pipelines.RetryBehavior;
import io.github.valossa515.spring_courier.core.pipelines.TransactionBehavior;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests for the dispatcher execution model: handlers run
 * inline on the calling thread (so transactions and other thread-bound
 * behaviors apply), handler exceptions propagate through the behavior
 * chain, and {@link Timeout} opts a request into off-thread execution.
 */
class CourierExecutionModelTest {

    static class ThreadCaptureRequest implements IRequest<String> {
    }

    @Timeout(5_000)
    static class OffloadedRequest implements IRequest<String> {
    }

    static class TxCmd implements ICommand<String> {
    }

    static class RetryCmd implements ICommand<String> {
    }

    @SuppressWarnings("unused")
    static class InlineThreadHandler {
        final AtomicReference<Thread> seenThread = new AtomicReference<>();

        public String handle(ThreadCaptureRequest r) {
            seenThread.set(Thread.currentThread());
            return "ok";
        }
    }

    @SuppressWarnings("unused")
    static class OffloadedThreadHandler {
        final AtomicReference<Thread> seenThread = new AtomicReference<>();

        public String handle(OffloadedRequest r) {
            seenThread.set(Thread.currentThread());
            return "ok";
        }
    }

    @Test
    void handlerRunsOnCallingThreadByDefault() {
        CourierTestFixture fixture = CourierTestFixture.create();
        InlineThreadHandler handler = new InlineThreadHandler();
        fixture.handlerRegistry().registerHandler(
                ThreadCaptureRequest.class, handler);

        Response<String> resp = fixture.courier()
                .send(new ThreadCaptureRequest());

        assertTrue(resp.isSuccess());
        assertSame(Thread.currentThread(), handler.seenThread.get(),
                "Plain requests must execute inline so thread-bound "
                        + "state (transactions, MDC) applies to the handler");
    }

    @Test
    void timeoutAnnotatedHandlerRunsOnSeparateThread() {
        CourierTestFixture fixture = CourierTestFixture.create();
        OffloadedThreadHandler handler = new OffloadedThreadHandler();
        fixture.handlerRegistry().registerHandler(
                OffloadedRequest.class, handler);

        Response<String> resp = fixture.courier()
                .send(new OffloadedRequest());

        assertTrue(resp.isSuccess());
        assertNotSame(Thread.currentThread(), handler.seenThread.get(),
                "@Timeout requests are offloaded to a watched thread");
    }

    @SuppressWarnings("unused")
    static class FailingTxHandler {
        public String handle(TxCmd cmd) {
            throw new IllegalStateException("db down");
        }
    }

    @SuppressWarnings("unused")
    static class ErrorResponseTxHandler {
        public Response<String> handle(TxCmd cmd) {
            return Response.error("domain failure", 422);
        }
    }

    @Test
    void transactionRollsBackWhenHandlerThrows() {
        PlatformTransactionManager txManager =
                mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(status);

        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.pipelineRegistry().registerGlobalBehavior(
                new TransactionBehavior<>(txManager), TxCmd.class);
        fixture.handlerRegistry().registerHandler(
                TxCmd.class, new FailingTxHandler());

        Response<String> resp = fixture.courier().send(new TxCmd());

        assertFalse(resp.isSuccess());
        assertEquals("An internal error occurred", resp.getError());
        verify(txManager).rollback(status);
        verify(txManager, never()).commit(any());
    }

    @Test
    void transactionRollsBackWhenHandlerReturnsErrorResponse() {
        PlatformTransactionManager txManager =
                mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(status);

        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.pipelineRegistry().registerGlobalBehavior(
                new TransactionBehavior<>(txManager), TxCmd.class);
        fixture.handlerRegistry().registerHandler(
                TxCmd.class, new ErrorResponseTxHandler());

        Response<String> resp = fixture.courier().send(new TxCmd());

        assertFalse(resp.isSuccess());
        assertEquals(422, resp.getStatusCode());
        verify(txManager).rollback(status);
        verify(txManager, never()).commit(any());
    }

    @SuppressWarnings("unused")
    static class FlakyHandler {
        final AtomicInteger attempts = new AtomicInteger();

        public String handle(RetryCmd cmd) {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("transient failure");
            }
            return "recovered";
        }
    }

    @SuppressWarnings("unused")
    static class AlwaysFailingHandler {
        final AtomicInteger attempts = new AtomicInteger();

        public String handle(RetryCmd cmd) {
            attempts.incrementAndGet();
            throw new IllegalStateException("permanent failure");
        }
    }

    @Test
    void retryRetriesHandlerExceptionsUntilSuccess() {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.pipelineRegistry().registerGlobalBehavior(
                new RetryBehavior<>(3, 1, 1.0), RetryCmd.class);
        FlakyHandler handler = new FlakyHandler();
        fixture.handlerRegistry().registerHandler(
                RetryCmd.class, handler);

        Response<String> resp = fixture.courier().send(new RetryCmd());

        assertTrue(resp.isSuccess());
        assertEquals("recovered", resp.getData());
        assertEquals(3, handler.attempts.get());
    }

    @Test
    void retryExhaustionReturnsErrorResponseInsteadOfThrowing() {
        CourierTestFixture fixture = CourierTestFixture.create();
        fixture.pipelineRegistry().registerGlobalBehavior(
                new RetryBehavior<>(2, 1, 1.0), RetryCmd.class);
        AlwaysFailingHandler handler = new AlwaysFailingHandler();
        fixture.handlerRegistry().registerHandler(
                RetryCmd.class, handler);

        Response<String> resp = fixture.courier().send(new RetryCmd());

        assertFalse(resp.isSuccess());
        assertEquals("An internal error occurred", resp.getError());
        assertEquals(2, handler.attempts.get());
    }

    @Test
    void exceptionHandlerInterceptsHandlerException() {
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        handlerRegistry.registerHandler(TxCmd.class, new FailingTxHandler());
        PipelineExecutor pipelineExecutor =
                new PipelineExecutor(new PipelineRegistry());

        ProcessorRegistry processorRegistry = new ProcessorRegistry();
        processorRegistry.registerExceptionHandler(
                (IRequestExceptionHandler<Object, Object>)
                        (request, exception) -> {
                            assertTrue(exception
                                            instanceof IllegalStateException,
                                    "Exception handlers must receive the "
                                            + "original handler exception");
                            return Response.error("intercepted", 409);
                        });

        Courier courier = new Courier(handlerRegistry,
                new NotificationRegistry(), pipelineExecutor,
                processorRegistry, null, 30_000,
                NotificationPublishingStrategy.SEQUENTIAL);

        Response<String> resp = courier.send(new TxCmd());

        assertFalse(resp.isSuccess());
        assertEquals(409, resp.getStatusCode());
        assertEquals("intercepted", resp.getError());
    }
}
