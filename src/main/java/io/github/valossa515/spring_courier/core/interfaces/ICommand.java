package io.github.valossa515.spring_courier.core.interfaces;
/**
 * Interface que representa um comando no padrão CQRS.
 * <p>
 * Estende {@link IRequest}, indicando que comandos também são requisições que produzem uma resposta do tipo especificado.
 * Utilizada para diferenciar comandos de queries em arquiteturas orientadas a mensagens.
 * </p>
 *
 * @param <R> Tipo da resposta retornada pelo comando
 */
public interface ICommand<R> extends IRequest<R> {
}
