package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.CourierContext;
import io.github.valossa515.spring_courier.core.support.CourierContextHolder;
import io.github.valossa515.spring_courier.core.support.Response;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TracingBehaviorTest {

    record TestCmd() implements ICommand<String> {
    }

    record TestQuery() implements IQuery<String> {
    }

    private Tracer tracer;
    private Span span;
    private SpanBuilder spanBuilder;
    private Scope scope;
    private TracingBehavior<IRequest<Response<?>>, Response<?>>
            behavior;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        span = mock(Span.class);
        spanBuilder = mock(SpanBuilder.class);
        scope = mock(Scope.class);

        when(tracer.spanBuilder(anyString()))
                .thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any(SpanKind.class)))
                .thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString()))
                .thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);

        behavior = new TracingBehavior<>(tracer);

        CourierContextHolder.setContext(
                CourierContext.create("trace-id"));
    }

    @AfterEach
    void cleanup() {
        CourierContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsSpanForCommand() {
        TestCmd cmd = new TestCmd();
        Response<?> result = behavior.handle(
                (IRequest<Response<?>>) (IRequest<?>) cmd,
                () -> Response.success("ok"));

        verify(tracer).spanBuilder("courier.TestCmd");
        verify(spanBuilder).setAttribute(
                "courier.request.category", "command");
        verify(spanBuilder).setAttribute(
                "courier.correlation.id", "trace-id");
        verify(span).end();
        verify(scope).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsSpanForQuery() {
        TestQuery query = new TestQuery();
        behavior.handle(
                (IRequest<Response<?>>) (IRequest<?>) query,
                () -> Response.success("data"));

        verify(spanBuilder).setAttribute(
                "courier.request.category", "query");
    }

    @Test
    @SuppressWarnings("unchecked")
    void setsErrorStatusOnErrorResponse() {
        TestCmd cmd = new TestCmd();
        behavior.handle(
                (IRequest<Response<?>>) (IRequest<?>) cmd,
                () -> Response.error("bad", 400));

        verify(span).setStatus(
                eq(StatusCode.ERROR), eq("bad"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void setsErrorStatusOnException() {
        TestCmd cmd = new TestCmd();
        RuntimeException ex = new RuntimeException("boom");

        assertThrows(RuntimeException.class,
                () -> behavior.handle(
                        (IRequest<Response<?>>)
                                (IRequest<?>) cmd,
                        () -> { throw ex; }));

        verify(span).setStatus(StatusCode.ERROR, "boom");
        verify(span).recordException(ex);
        verify(span).end();
    }

    @Test
    @SuppressWarnings("unchecked")
    void successResponseDoesNotSetError() {
        TestCmd cmd = new TestCmd();
        behavior.handle(
                (IRequest<Response<?>>) (IRequest<?>) cmd,
                () -> Response.success("ok"));

        verify(span, never()).setStatus(
                eq(StatusCode.ERROR), anyString());
    }

    @Test
    void orderIsHighestPrecedencePlus10() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 10,
                behavior.getOrder());
    }
}
