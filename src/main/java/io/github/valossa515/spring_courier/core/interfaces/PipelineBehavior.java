package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Interface que define um comportamento de pipeline para processamento de requisições no padrão CQRS.
 * <p>
 * Permite adicionar lógica intermediária (como validação, logging, autorização, etc.) ao fluxo de manipulação de comandos e queries.
 * Implementações podem interceptar, modificar ou delegar o processamento da requisição ao próximo comportamento no pipeline.
 * </p>
 *
 * @param <T> Tipo da requisição, que deve implementar {@link IRequest}
 * @param <R> Tipo da resposta retornada pelo pipeline
 */
public interface PipelineBehavior<T extends IRequest<R>, R> {
    /**
     * Processa a requisição recebida, podendo executar lógica adicional antes ou depois de delegar ao próximo comportamento.
     *
     * @param request requisição a ser processada
     * @param next referência para o próximo comportamento no pipeline
     * @return resposta resultante do processamento
     */
    R handle(T request, Next<R> next);

    /**
     * Interface que representa o próximo passo no pipeline de processamento.
     *
     * @param <R> Tipo da resposta retornada
     */
    interface Next<R> {
        /**
         * Invoca o próximo comportamento no pipeline.
         *
         * @return resposta resultante do próximo processamento
         */
        R invoke();
    }
}
