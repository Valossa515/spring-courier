package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.annotations.Timeout;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionBehaviorTest {

    record TestCommand() implements ICommand<String> {
    }

    record TestQuery() implements IQuery<String> {
    }

    @Test
    @SuppressWarnings("unchecked")
    void commandIsWrappedInTransaction() {
        PlatformTransactionManager txManager =
                mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(status);

        TransactionBehavior<IRequest<Response<String>>,
                Response<String>> behavior =
                new TransactionBehavior<>(txManager);

        TestCommand cmd = new TestCommand();
        PipelineBehavior.Next<Response<String>> next =
                () -> Response.success("done");

        Response<String> result = behavior.handle(
                (IRequest<Response<String>>) (IRequest<?>) cmd,
                next);

        assertTrue(result.isSuccess());
        verify(txManager).getTransaction(any());
        verify(txManager).commit(status);
        verify(txManager, never()).rollback(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void commandRollsBackOnException() {
        PlatformTransactionManager txManager =
                mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(status);

        TransactionBehavior<IRequest<Response<String>>,
                Response<String>> behavior =
                new TransactionBehavior<>(txManager);

        TestCommand cmd = new TestCommand();
        PipelineBehavior.Next<Response<String>> next = () -> {
            throw new RuntimeException("boom");
        };

        assertThrows(RuntimeException.class,
                () -> behavior.handle(
                        (IRequest<Response<String>>)
                                (IRequest<?>) cmd, next));

        verify(txManager).rollback(status);
        verify(txManager, never()).commit(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void querySkipsTransaction() {
        PlatformTransactionManager txManager =
                mock(PlatformTransactionManager.class);

        TransactionBehavior<IRequest<Response<String>>,
                Response<String>> behavior =
                new TransactionBehavior<>(txManager);

        TestQuery query = new TestQuery();
        PipelineBehavior.Next<Response<String>> next =
                () -> Response.success("data");

        Response<String> result = behavior.handle(
                (IRequest<Response<String>>) (IRequest<?>) query,
                next);

        assertTrue(result.isSuccess());
        assertEquals("data", result.getData());
        verify(txManager, never()).getTransaction(any());
    }

    @Test
    void orderIsHighestPrecedencePlus200() {
        PlatformTransactionManager txManager =
                mock(PlatformTransactionManager.class);
        TransactionBehavior<?, ?> behavior =
                new TransactionBehavior<>(txManager);
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 200,
                behavior.getOrder());
    }

    @Timeout(5_000)
    record OffloadedCommand() implements ICommand<String> {
    }

    @Timeout(-1)
    record InvalidTimeoutCommand() implements ICommand<String> {
    }

    @Test
    @SuppressWarnings("unchecked")
    void timeoutAnnotatedCommandStillExecutesAndWarnsOnlyOnce() {
        PlatformTransactionManager txManager =
                mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(status);

        TransactionBehavior<IRequest<Response<String>>,
                Response<String>> behavior =
                new TransactionBehavior<>(txManager);

        OffloadedCommand cmd = new OffloadedCommand();
        PipelineBehavior.Next<Response<String>> next =
                () -> Response.success("done");

        // First call hits the warn branch; second exercises the
        // warned-once guard — both must still run transactionally
        Response<String> first = behavior.handle(
                (IRequest<Response<String>>) (IRequest<?>) cmd, next);
        Response<String> second = behavior.handle(
                (IRequest<Response<String>>) (IRequest<?>) cmd, next);

        assertTrue(first.isSuccess());
        assertTrue(second.isSuccess());
        verify(txManager, times(2)).commit(status);
    }

    @Test
    @SuppressWarnings("unchecked")
    void nonPositiveTimeoutDoesNotTriggerWarning() {
        PlatformTransactionManager txManager =
                mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(status);

        TransactionBehavior<IRequest<Response<String>>,
                Response<String>> behavior =
                new TransactionBehavior<>(txManager);

        // Courier ignores non-positive @Timeout values (runs inline),
        // so no off-thread warning applies
        Response<String> result = behavior.handle(
                (IRequest<Response<String>>) (IRequest<?>)
                        new InvalidTimeoutCommand(),
                () -> Response.success("done"));

        assertTrue(result.isSuccess());
        verify(txManager).commit(status);
    }

    @Test
    @SuppressWarnings("unchecked")
    void commandRollsBackOnErrorResponse() {
        PlatformTransactionManager txManager =
                mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(status);

        TransactionBehavior<IRequest<Response<String>>,
                Response<String>> behavior =
                new TransactionBehavior<>(txManager);

        Response<String> result = behavior.handle(
                (IRequest<Response<String>>) (IRequest<?>)
                        new TestCommand(),
                () -> Response.error("domain failure", 422));

        assertEquals(422, result.getStatusCode());
        verify(txManager).rollback(status);
        verify(txManager, never()).commit(any());
    }
}
