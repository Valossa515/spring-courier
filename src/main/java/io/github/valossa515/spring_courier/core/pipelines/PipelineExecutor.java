package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Coordinates the execution of registered {@link PipelineBehavior} instances
 * and normalizes the final result as a {@link Response}.
 */
public class PipelineExecutor {
    private static final Logger logger = LoggerFactory.getLogger(PipelineExecutor.class);
    private final PipelineRegistry pipelineRegistry;

    public PipelineExecutor(PipelineRegistry pipelineRegistry) {
        this.pipelineRegistry = pipelineRegistry;
    }

    /**
     * Executes the request through the behavior chain and normalizes the
     * outcome as a {@link Response}.
     *
     * @param <R>            request type passed to the pipeline
     * @param <S>            response type expected from the handler
     * @param request        request instance to process
     * @param handlerInvoker callback that invokes the underlying handler
     * @return normalized response produced by the handler or behaviors
     */
    @SuppressWarnings("unchecked")
    public <R extends IRequest<S>, S> Response<S> execute(
            R request,
            HandlerInvoker<R, S> handlerInvoker) {

        String type = detectRequestCategory(request);
        logger.debug("Executando pipeline para {}: {}", type, request.getClass().getSimpleName());

        List<PipelineBehavior<R, S>> behaviors =
                pipelineRegistry.getBehaviors((Class<R>) request.getClass());

        Object result = behaviors.isEmpty()
                ? handlerInvoker.invoke()
                : executeBehaviorChain(request, behaviors, 0, handlerInvoker);

        return normalize(result);
    }

    private String detectRequestCategory(Object request) {
        Class<?> clazz = request.getClass();
        if (implementsInterface(clazz, "ICommand")) return "🟢 Command";
        if (implementsInterface(clazz, "IQuery")) return "🔵 Query";
        return "⚪ Generic IRequest";
    }

    private boolean implementsInterface(Class<?> clazz, String interfaceNamePart) {
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.getSimpleName().contains(interfaceNamePart)) return true;
        }
        if (clazz.getSuperclass() != null) {
            return implementsInterface(clazz.getSuperclass(), interfaceNamePart);
        }
        return false;
    }

    @FunctionalInterface
    @SuppressWarnings("java:S2326")
    public interface HandlerInvoker<R extends IRequest<S>, S> {

        /**
         * Invokes the handler associated with the request and returns either a
         * raw value or a {@link Response} instance.
         *
         * @return handler result to normalize
         */
        Object invoke();
    }

    /**
     * Executes the registered behaviors recursively.
     */
    @SuppressWarnings("unchecked")
    private <R extends IRequest<S>, S> Object executeBehaviorChain(
            R request,
            List<PipelineBehavior<R, S>> behaviors,
            int currentIndex,
            HandlerInvoker<R, S> handlerInvoker) {

        if (currentIndex >= behaviors.size()) {
            return handlerInvoker.invoke();
        }

        PipelineBehavior<R, S> current = behaviors.get(currentIndex);
        return current.handle(request, () ->
                (S) executeBehaviorChain(request, behaviors, currentIndex + 1, handlerInvoker)
        );
    }

    /**
     * Converts the handler output to {@code Response<S>} when needed.
     */
    @SuppressWarnings("unchecked")
    private <S> Response<S> normalize(Object result) {
        if (result instanceof Response<?>) {
            return (Response<S>) result;
        }
        return Response.success((S) result);
    }
}