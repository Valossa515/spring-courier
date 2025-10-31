package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Responsável por orquestrar a execução das requisições através da cadeia de
 * {@link PipelineBehavior}s registrados, garantindo que o resultado final seja
 * normalizado para {@link Response}. Esta classe coordena a chamada dos
 * behaviors e do handler final, permitindo que preocupações transversais sejam
 * aplicadas de forma consistente.
 */
public class PipelineExecutor {
    private static final Logger logger = LoggerFactory.getLogger(PipelineExecutor.class);
    private final PipelineRegistry pipelineRegistry;

    public PipelineExecutor(PipelineRegistry pipelineRegistry) {
        this.pipelineRegistry = pipelineRegistry;
    }

    /**
     * Executa o request através dos behaviors, garantindo compatibilidade entre
     * handlers que retornam TResponse e os que retornam Response<TResponse>.
     */
    @SuppressWarnings("unchecked")
    public <TRequest extends IRequest<TResponse>, TResponse> Response<TResponse> execute(
            TRequest request,
            HandlerInvoker<TRequest, TResponse> handlerInvoker) {

        String type = detectRequestCategory(request);
        logger.debug("Executando pipeline para {}: {}", type, request.getClass().getSimpleName());

        List<PipelineBehavior<TRequest, TResponse>> behaviors =
                pipelineRegistry.getBehaviors((Class<TRequest>) request.getClass());

        // 🔹 Executa handler diretamente ou via cadeia de behaviors
        Object result = behaviors.isEmpty()
                ? handlerInvoker.invoke()
                : executeBehaviorChain(request, behaviors, 0, handlerInvoker);

        // 🔹 Normaliza o retorno (Response<T> ou valor bruto)
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
         * Executa o handler associado ao request, retornando diretamente o
         * resultado produzido. Pode ser tanto o valor cru quanto um Response<T>.
         */
        Object invoke();
    }

    /**
     * Executa recursivamente a cadeia de behaviors registrada.
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
     * Converte automaticamente qualquer retorno para Response<TResponse>.
     */
    @SuppressWarnings("unchecked")
    private <TResponse> Response<TResponse> normalize(Object result) {
        if (result instanceof Response<?> response) {
            return (Response<TResponse>) response;
        }
        return Response.success((TResponse) result);
    }
}