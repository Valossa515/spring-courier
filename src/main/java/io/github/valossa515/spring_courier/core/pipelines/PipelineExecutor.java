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

        List<PipelineBehavior<TRequest, TResponse>> behaviors =
                pipelineRegistry.getBehaviors((Class<TRequest>) request.getClass());

        Response<TResponse> response;

        if (behaviors.isEmpty()) {
            logger.debug("Nenhum behavior encontrado para request: {}", request.getClass().getSimpleName());
            response = normalize(handlerInvoker.invoke());
        } else {
            logger.debug("Executando {} behaviors para request: {}",
                    behaviors.size(), request.getClass().getSimpleName());
            response = (Response<TResponse>) executeBehaviorChain(request, behaviors, 0, handlerInvoker);
        }

        return response;
    }

    @FunctionalInterface
    public interface HandlerInvoker<TRequest extends IRequest<TResponse>, TResponse> {

        /**
         * Executa o handler associado ao request, retornando diretamente o
         * resultado produzido. A implementação pode retornar tanto a resposta
         * concreta quanto um {@link Response} já padronizado.
         *
         * @return instância de {@link Response} ou o valor bruto de TResponse a
         * ser normalizado.
         */
        Object invoke();
    }

    private <TRequest extends IRequest<TResponse>, TResponse> TResponse executeBehaviorChain(
            TRequest request,
            List<PipelineBehavior<TRequest, TResponse>> behaviors,
            int currentIndex,
            HandlerInvoker<TRequest, TResponse> handlerInvoker) {

        if (currentIndex >= behaviors.size()) {
            return (TResponse) normalize(handlerInvoker.invoke());
        }

        PipelineBehavior<TRequest, TResponse> currentBehavior = behaviors.get(currentIndex);
        return currentBehavior.handle(request, () ->
                executeBehaviorChain(request, behaviors, currentIndex + 1, handlerInvoker)
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
