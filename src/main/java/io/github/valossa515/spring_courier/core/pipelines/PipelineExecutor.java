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
     * @param <TRequest>     request type passed to the pipeline
     * @param <TResponse>    response type expected from the handler
     * @param request        request instance to process
     * @param handlerInvoker callback that invokes the underlying handler
     * @return normalized response produced by the handler or behaviors
     */
    @SuppressWarnings("unchecked")
    public <TRequest extends IRequest<TResponse>, TResponse> Response<TResponse> execute(
            TRequest request,
            HandlerInvoker<TRequest, TResponse> handlerInvoker) {

        String type = detectRequestCategory(request);
        logger.debug("Executando pipeline para {}: {}", type, request.getClass().getSimpleName());

        List<PipelineBehavior<TRequest, TResponse>> behaviors =
                pipelineRegistry.getBehaviors((Class<TRequest>) request.getClass());

        // 🔹 Execute the handler directly or via the behavior chain
        Object result = behaviors.isEmpty()
                ? handlerInvoker.invoke()
                : executeBehaviorChain(request, behaviors, 0, handlerInvoker);

        // 🔹 Normalize the return value (Response<T> or raw value)
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
    public interface HandlerInvoker<TRequest extends IRequest<TResponse>, TResponse> {

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
    private <TRequest extends IRequest<TResponse>, TResponse> Object executeBehaviorChain(
            TRequest request,
            List<PipelineBehavior<TRequest, TResponse>> behaviors,
            int currentIndex,
            HandlerInvoker<TRequest, TResponse> handlerInvoker) {

        if (currentIndex >= behaviors.size()) {
            return handlerInvoker.invoke();
        }

        PipelineBehavior<TRequest, TResponse> current = behaviors.get(currentIndex);
        return current.handle(request, () ->
                (TResponse) executeBehaviorChain(request, behaviors, currentIndex + 1, handlerInvoker)
        );
    }

    /**
     * Converts the handler output to {@code Response<TResponse>} when needed.
     */
    @SuppressWarnings("unchecked")
    private <TResponse> Response<TResponse> normalize(Object result) {
        if (result instanceof Response<?>) {
            return (Response<TResponse>) result;
        }
        return Response.success((TResponse) result);
    }
}
