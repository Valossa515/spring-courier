package io.github.valossa515.spring_courier.core.interfaces;

/**
 * Interface base para requisições no padrão CQRS.
 * <p>
 * Representa uma operação que produz uma resposta do tipo especificado.
 * Utilizada como supertipo para comandos ({@link ICommand}) e consultas ({@link IQuery}),
 * permitindo abstração e tratamento genérico de requisições.
 * </p>
 *
 * @param <R> Tipo da resposta retornada pela requisição
 */
public interface IRequest<R> {
}
