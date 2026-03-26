package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.CourierContextHolder;
import io.github.valossa515.spring_courier.core.support.Response;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.core.Ordered;

/**
 * Pipeline behavior that creates OpenTelemetry spans for every
 * request dispatched through the Courier pipeline.
 *
 * <p>Each span records:
 * <ul>
 *   <li>{@code courier.request.type} — simple class name</li>
 *   <li>{@code courier.request.category} — command / query / request</li>
 *   <li>{@code courier.correlation.id} — from CourierContextHolder</li>
 * </ul>
 *
 * <p>Activated automatically when OpenTelemetry API is on the classpath
 * and a {@link Tracer} bean is present. Disable with
 * {@code spring.courier.tracing.enabled=false}.
 *
 * @param <R> request type
 * @param <S> response type
 */
public class TracingBehavior<R extends IRequest<S>, S>
        implements PipelineBehavior<R, S>, Ordered {

    private static final String INSTRUMENTATION_NAME =
            "spring-courier";

    private final Tracer tracer;

    public TracingBehavior(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public S handle(R request, Next<S> next) {
        String requestName = request.getClass().getSimpleName();
        String category = resolveCategory(request);

        Span span = tracer.spanBuilder("courier." + requestName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("courier.request.type", requestName)
                .setAttribute("courier.request.category", category)
                .setAttribute("courier.correlation.id",
                        CourierContextHolder.getContext()
                                .getCorrelationId())
                .startSpan();

        try (var scope = span.makeCurrent()) {
            S result = next.invoke();

            if (result instanceof Response<?> response) {
                span.setAttribute("courier.response.success",
                        response.isSuccess());
                span.setAttribute("courier.response.status",
                        response.getStatusCode());
                if (!response.isSuccess()) {
                    span.setStatus(StatusCode.ERROR,
                            response.getError() != null
                                    ? response.getError()
                                    : "unknown error");
                }
            }

            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            span.recordException(ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private String resolveCategory(R request) {
        if (request instanceof ICommand<?>) {
            return "command";
        }
        if (request instanceof IQuery<?>) {
            return "query";
        }
        return "request";
    }
}
