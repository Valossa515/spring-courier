package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PipelineExecutor {
    private static final Logger logger = LoggerFactory.getLogger(PipelineExecutor.class);

    private final PipelineRegistry pipelineRegistry;

    public PipelineExecutor(PipelineRegistry pipelineRegistry) {
        this.pipelineRegistry = pipelineRegistry;
    }

    /**
     * Executa o request através do pipeline de behaviors
     */
    public <TRequest extends IRequest<TResponse>, TResponse> TResponse execute(
            TRequest request,
            HandlerInvoker<TRequest, TResponse> handlerInvoker) {

        List<PipelineBehavior<TRequest, TResponse>> behaviors =
                pipelineRegistry.getBehaviors((Class<TRequest>) request.getClass());

        if (behaviors.isEmpty()) {
            logger.debug("No behaviors found for request: {}", request.getClass().getSimpleName());
            return handlerInvoker.invoke();
        }

        logger.debug("Executing {} behaviors for request: {}",
                behaviors.size(), request.getClass().getSimpleName());

        // Executa a chain de behaviors recursivamente
        return executeBehaviorChain(request, behaviors, 0, handlerInvoker);
    }

    /**
     * Interface para invocar o handler final
     */
    @FunctionalInterface
    public interface HandlerInvoker<TRequest extends IRequest<TResponse>, TResponse> {
        TResponse invoke();
    }

    /**
     * Método recursivo que executa a chain de behaviors
     */
    private <TRequest extends IRequest<TResponse>, TResponse> TResponse executeBehaviorChain(
            TRequest request,
            List<PipelineBehavior<TRequest, TResponse>> behaviors,
            int currentIndex,
            HandlerInvoker<TRequest, TResponse> handlerInvoker) {

        // Se chegou ao final da chain, invoca o handler
        if (currentIndex >= behaviors.size()) {
            return handlerInvoker.invoke();
        }

        // Pega o behavior atual
        PipelineBehavior<TRequest, TResponse> currentBehavior = behaviors.get(currentIndex);

        // Invoca o behavior atual, passando uma função para o próximo
        return currentBehavior.handle(request, () ->
                executeBehaviorChain(request, behaviors, currentIndex + 1, handlerInvoker)
        );
    }
}