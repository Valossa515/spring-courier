package io.github.valossa515.spring_courier.core.exceptions;

/**
 * Exceção lançada quando não há um handler registrado para processar um
 * determinado request. Permite que os consumidores detectem configurações
 * incompletas do pipeline e reajam registrando o handler apropriado ou
 * tratando o cenário de ausência.
 *
 * <p>Uso típico:</p>
 *
 * <pre>
 * try {
 *     pipelineExecutor.execute(request, () -> handlerRegistry.invoke(request));
 * } catch (HandlerNotFoundException ex) {
 *     logger.error("Handler ausente para {}", request.getClass(), ex);
 *     throw new IllegalStateException("Configure os handlers antes de processar", ex);
 * }
 * </pre>
 */
public class HandlerNotFoundException extends RuntimeException {

    /**
     * Cria a exceção informando apenas a mensagem descritiva do problema.
     *
     * @param message descrição do motivo pelo qual o handler não foi encontrado.
     */
    public HandlerNotFoundException(String message) {
        super(message);
    }

    /**
     * Cria a exceção permitindo associar uma causa original que levou à
     * ausência do handler.
     *
     * @param message descrição do motivo pelo qual o handler não foi encontrado.
     * @param cause   exceção raiz que originou o problema.
     */
    public HandlerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
