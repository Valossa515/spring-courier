package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Interface funcional para manipulação de comandos no padrão CQRS.
 * <p>
 * Implementações desta interface devem processar um comando do tipo {@link IRequest}
 * e retornar uma resposta do tipo especificado.
 * </p>
 *
 * @param <C> Tipo do comando, que deve implementar {@link IRequest}
 * @param <R> Tipo da resposta retornada pelo handler
 */
@FunctionalInterface
public interface CommandHandler<C extends IRequest<R>, R>{
    /**
     * Processa o comando recebido e retorna uma resposta.
     *
     * @param command comando a ser processado
     * @return resposta resultante do processamento
     */
    R handle(C command);
}