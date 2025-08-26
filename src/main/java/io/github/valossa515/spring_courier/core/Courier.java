package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Componente responsável pelo envio e roteamento de requisições no padrão CQRS.
 * <p>
 * Utiliza o {@link HandlerRegistry} para localizar e invocar o handler apropriado para cada requisição ({@link IRequest}).
 * Realiza o despacho dinâmico de comandos e queries, registrando logs de execução e tempo de processamento.
 * </p>
 *
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * Courier courier = ...;
 * MinhaResposta resposta = courier.send(new MinhaRequest());
 * }
 * </pre>
 * </p>
 *
 * @author Valossa515
 */
@Component
public class Courier {
    private static final Logger logger = LoggerFactory.getLogger(Courier.class);
    private final HandlerRegistry handlerRegistry;

    public Courier(HandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
        logger.info("Courier inicializado com {} handlers registrados", handlerRegistry.getHandlerCount());
    }

    /**
     * Envia uma requisição para o handler correspondente e retorna a resposta.
     *
     * @param request requisição a ser processada
     * @param <T> tipo da resposta esperada
     * @return resposta do handler
     * @throws RuntimeException se ocorrer erro na execução do handler
     */
    @SuppressWarnings("unchecked")
    public <T> T send(IRequest<T> request) {
        logger.debug("Enviando request: {}", request.getClass().getSimpleName());

        Object handler = handlerRegistry.getHandler(request.getClass());
        return (T) invokeHandler(handler, request);
    }

    @SuppressWarnings("unchecked")
    private <R> R invokeHandler(Object handler, IRequest<R> request) {
        try {
            Method handleMethod = findHandleMethod(handler.getClass());
            long startTime = System.currentTimeMillis();

            R result = (R) handleMethod.invoke(handler, request);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Handler executado em {}ms: {} -> {}",
                    duration, request.getClass().getSimpleName(), handler.getClass().getSimpleName());

            return result;
        } catch (InvocationTargetException e) {
            logger.error("Erro na execução do handler: {}", e.getTargetException().getMessage(), e.getTargetException());
            throw new RuntimeException("Handler execution failed: " + e.getTargetException().getMessage(), e.getTargetException());
        } catch (Exception e) {
            logger.error("Erro ao invocar handler: {}", e.getMessage(), e);
            throw new RuntimeException("Error invoking handler: " + e.getMessage(), e);
        }
    }

    private Method findHandleMethod(Class<?> handlerClass) {
        for (Method method : handlerClass.getMethods()) {
            if (method.getName().equals("handle") && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new RuntimeException("No handle method found in handler: " + handlerClass.getName());
    }

    /**
     * Retorna a quantidade de handlers registrados no sistema.
     *
     * @return número de handlers registrados
     */
    public int getRegisteredHandlersCount() {
        return handlerRegistry.getHandlerCount();
    }
}