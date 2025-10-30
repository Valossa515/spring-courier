package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;

/**
 * Representa um comportamento intermediário dentro do pipeline de processamento
 * de requisições. Cada implementação pode executar ações antes ou depois da
 * invocação do próximo elemento da cadeia, permitindo adicionar
 * funcionalidades transversais como logging, validação ou auditoria.
 *
 * @param <TRequest>  tipo da requisição, que deve implementar {@link IRequest}
 *                    com o tipo de resposta correspondente.
 * @param <TResponse> tipo de resposta esperado para a requisição em questão.
 */
public interface PipelineBehavior<TRequest extends IRequest<TResponse>, TResponse> {

    /**
     * Manipula a requisição atual e decide quando (ou se) delegar a execução ao
     * próximo elemento da cadeia.
     *
     * @param request requisição recebida pelo pipeline.
     * @param next    invocador responsável por seguir para o próximo behavior ou
     *                handler final.
     * @return a resposta resultante da execução do pipeline.
     */
    TResponse handle(TRequest request, Next<TResponse> next);

    /**
     * Contrato que encapsula a invocação do próximo comportamento registrado ou
     * do handler final. Usado para compor a cadeia de execução do pipeline.
     *
     * @param <TResponse> tipo da resposta esperada após a invocação do próximo
     *                    comportamento.
     */
    interface Next<TResponse> {

        /**
         * Executa o próximo comportamento registrado ou o handler final do
         * pipeline.
         *
         * @return a resposta produzida pela continuação da cadeia.
         */
        TResponse invoke();
    }
}
