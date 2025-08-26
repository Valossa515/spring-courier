package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.exceptions.HandlerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de handlers para diferentes tipos de requisições.
 * <p>
 * Permite registrar, recuperar e verificar handlers associados a tipos específicos de requisições.
 * Utiliza um {@link ConcurrentHashMap} para garantir segurança em ambientes concorrentes.
 * </p>
 *
 * Métodos principais:
 * <ul>
 *   <li>{@link #registerHandler(Class, Object)}: Registra um handler para um tipo de requisição.</li>
 *   <li>{@link #getHandler(Class)}: Recupera o handler associado ao tipo de requisição informado.</li>
 *   <li>{@link #hasHandlerFor(Class)}: Verifica se existe um handler para o tipo de requisição.</li>
 *   <li>{@link #getHandlerCount()}: Retorna a quantidade de handlers registrados.</li>
 *   <li>{@link #getHandlers()}: Retorna um mapa com todos os handlers registrados.</li>
 * </ul>
 *
 * Os logs são gerenciados via SLF4J.
 */
public class HandlerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(HandlerRegistry.class);
    private final Map<Class<?>, Object> handlers = new ConcurrentHashMap<>();

    public void registerHandler(Class<?> requestType, Object handler) {
        if (handlers.containsKey(requestType)) {
            logger.warn("Substituindo handler existente para request type: {}", requestType.getSimpleName());
        }
        handlers.put(requestType, handler);
        logger.debug("Handler registrado para: {}", requestType.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public <T> Object getHandler(Class<T> requestType) {
        Object handler = handlers.get(requestType);
        if (handler == null) {
            String errorMsg = "No handler registered for request type: " + requestType.getName();
            logger.error(errorMsg);
            throw new HandlerNotFoundException(errorMsg);
        }
        logger.debug("Handler encontrado para: {}", requestType.getSimpleName());
        return handler;
    }

    public boolean hasHandlerFor(Class<?> requestType) {
        return handlers.containsKey(requestType);
    }

    public int getHandlerCount() {
        return handlers.size();
    }

    public Map<Class<?>, Object> getHandlers() {
        return new ConcurrentHashMap<>(handlers);
    }
}
