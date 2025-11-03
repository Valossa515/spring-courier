package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;

/**
 * Representa um comportamento intermediário dentro do pipeline de processamento
 * de requisições. Cada implementação pode executar ações antes ou depois da
 * invocação do próximo elemento da cadeia, permitindo adicionar
 * funcionalidades transversais como logging, validação ou auditoria.
 *
 * <p>Exemplo de behavior que registra tempo de execução ao redor do handler:</p>
 *
 * <pre>{@code
 * public class LoggingBehavior implements PipelineBehavior<CreateUserCommand, Response<UserDto>> {
 *     private static final Logger logger = LoggerFactory.getLogger(LoggingBehavior.class);
 *
 *     @Override
 *     public Response<UserDto> handle(CreateUserCommand request, Next<Response<UserDto>> next) {
 *         long startedAt = System.nanoTime();
 *         Response<UserDto> response = next.invoke();
 *         logger.debug("CreateUserCommand processado em {} nanos", System.nanoTime() - startedAt);
 *         return response;
 *     }
 * }
 * </pre>
 *
 * @param &lt;TRequest&gt;  tipo da requisição, que deve implementar {@link IRequest}
 *                        com o tipo de resposta correspondente.
 * @param &lt;TResponse&gt; tipo de resposta esperado para a requisição em questão.
 */
public interface PipelineBehavior<TRequest extends IRequest<TResponse>, TResponse> {

    /**
     * Manipula a requisição atual e decide quando (ou se) delegar a execução ao
     * próximo elemento da cadeia.
     *
     * <p>Dentro da implementação é comum capturar o retorno do {@code next}
     * para aplicar transformações ou lidar com exceções:</p>
     *
     * <pre>{@code
     * public Response<UserDto> handle(CreateUserCommand request, Next<Response<UserDto>> next) {
     *     try {
     *         return next.invoke();
     *     } catch (ConstraintViolationException exception) {
     *         return Response.failure(exception.getMessage());
     *     }
     * }
     * </pre>
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
     * <p>O método {@link #invoke()} deve ser chamado exatamente uma vez, a menos
     * que o behavior deseje interromper o fluxo. Por exemplo:</p>
     *
     * <pre>{@code
     * public Response<UserDto> handle(CreateUserCommand request, Next<Response<UserDto>> next) {
     *     if (!request.isEnabled()) {
     *         return Response.failure("Operação desabilitada");
     *     }
     *     return next.invoke();
     * }
     * </pre>
     *
     * @param &lt;TResponse&gt; tipo da resposta esperada após a invocação do próximo
     *                        comportamento.
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
