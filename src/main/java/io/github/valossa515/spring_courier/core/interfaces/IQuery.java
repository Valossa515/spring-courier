package io.github.valossa515.spring_courier.core.interfaces;
/**
 * Interface que representa uma consulta (query) no padrão CQRS.
 * <p>
 * Estende {@link IRequest}, indicando que queries são requisições que produzem uma resposta do tipo especificado.
 * Utilizada para diferenciar consultas de comandos em arquiteturas orientadas a mensagens.
 * </p>
 *
 * @param <R> Tipo da resposta retornada pela consulta
 */
public interface IQuery<R> extends IRequest<R>{
}
