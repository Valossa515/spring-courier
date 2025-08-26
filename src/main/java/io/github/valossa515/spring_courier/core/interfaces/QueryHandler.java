package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Interface funcional para manipulação de consultas (queries) no padrão CQRS.
 * <p>
 * Implementações desta interface devem processar uma consulta do tipo {@link IRequest}
 * e retornar uma resposta do tipo especificado.
 * Utilizada para separar o tratamento de queries dos comandos em arquiteturas orientadas a mensagens.
 * </p>
 *
 * @param <Q> Tipo da consulta, que deve implementar {@link IRequest}
 * @param <R> Tipo da resposta retornada pelo handler
 */
@FunctionalInterface
public interface QueryHandler<Q extends IRequest<R>, R>{
    /**
     * Processa a consulta recebida e retorna uma resposta.
     *
     * @param query consulta a ser processada
     * @return resposta resultante do processamento
     */
    R handle(Q query);
}
