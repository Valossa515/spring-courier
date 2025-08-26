package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.annotations.ExposeHandler;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BeanPostProcessor responsável por descobrir e registrar handlers de comandos e queries
 * durante a inicialização dos beans no contexto Spring.
 * <p>
 * Ele identifica beans anotados com {@link io.github.valossa515.spring_courier.annotations.ExposeHandler}
 * ou que implementam interfaces de handler, e registra esses handlers no {@link HandlerRegistry}.
 * </p>
 *
 * @author Valossa515
 */
public class HandlerDiscoveryPostProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(HandlerDiscoveryPostProcessor.class);
    private final Map<Class<?>, Boolean> handlerInterfaceCache = new ConcurrentHashMap<>();

    private final HandlerRegistry handlerRegistry;

    /**
     * Construtor da classe.
     *
     * @param handlerRegistry     Registro de handlers onde os handlers descobertos serão registrados.
     * @param applicationContext Contexto da aplicação Spring.
     */
    public HandlerDiscoveryPostProcessor(HandlerRegistry handlerRegistry, ApplicationContext applicationContext) {
        this.handlerRegistry = handlerRegistry;
    }


    /**
     * Processa o bean após sua inicialização, verificando se ele deve ser registrado como handler.
     *
     * @param bean     Instância do bean.
     * @param beanName Nome do bean.
     * @return O bean original.
     * @throws BeansException Em caso de erro no processamento do bean.
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, @NotNull String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        if (shouldProcessBean(beanClass)) {
            discoverAndRegisterHandler(bean, beanClass);
        }

        return bean;
    }

    /**
     * Verifica se o bean deve ser processado como handler.
     *
     * @param beanClass Classe do bean.
     * @return true se deve ser processado, false caso contrário.
     */
    private boolean shouldProcessBean(Class<?> beanClass) {
        return beanClass.isAnnotationPresent(ExposeHandler.class) ||
                isHandlerInterface(beanClass);
    }

    /**
     * Verifica se a classe implementa uma interface de handler.
     *
     * @param beanClass Classe do bean.
     * @return true se implementa, false caso contrário.
     */
    private boolean isHandlerInterface(Class<?> beanClass) {
        // Verifica interfaces implementadas
        for (Class<?> iface : beanClass.getInterfaces()) {
            if (isHandlerInterfaceType(iface)) {
                return true;
            }
        }

        // Verifica superclasse
        Class<?> superclass = beanClass.getSuperclass();
        return superclass != null && isHandlerInterfaceType(superclass);
    }

    /**
     * Verifica se o tipo é uma interface de handler conhecida.
     *
     * @param interfaceType Tipo da interface.
     * @return true se for interface de handler, false caso contrário.
     */
    private boolean isHandlerInterfaceType(Class<?> interfaceType) {
        return handlerInterfaceCache.computeIfAbsent(interfaceType, this::checkIfHandlerInterface);
    }

    /**
     * Checa se a interface é um handler padrão ou customizado.
     *
     * @param interfaceType Tipo da interface.
     * @return true se for handler, false caso contrário.
     */
    private boolean checkIfHandlerInterface(Class<?> interfaceType) {
        // Interfaces padrão da biblioteca
        if (CommandHandler.class.isAssignableFrom(interfaceType) ||
                QueryHandler.class.isAssignableFrom(interfaceType)) {
            logger.debug("Interface identificada como handler padrão: {}", interfaceType.getSimpleName());
            return true;
        }

        // Verifica se é uma interface customizada com método handle
        return hasValidHandleMethod(interfaceType);
    }

    /**
     * Verifica se a interface possui um método handle válido.
     *
     * @param interfaceType Tipo da interface.
     * @return true se possui método handle, false caso contrário.
     */
    private boolean hasValidHandleMethod(Class<?> interfaceType) {
        try {
            Method[] methods = interfaceType.getMethods();
            for (Method method : methods) {
                if ("handle".equals(method.getName()) &&
                        method.getParameterCount() == 1 &&
                        method.getReturnType() != Void.TYPE) {

                    logger.debug("Interface {} identificada como handler via método: {}",
                            interfaceType.getSimpleName(), method.toGenericString());
                    return true;
                }
            }
        } catch (SecurityException e) {
            logger.warn("Erro de segurança ao analisar interface {}: {}",
                    interfaceType.getSimpleName(), e.getMessage());
            return false;
        }

        logger.trace("Interface {} não contém método handle válido", interfaceType.getSimpleName());
        return false;
    }

    /**
     * Descobre e registra o handler no registry.
     *
     * @param bean     Instância do bean.
     * @param beanClass Classe do bean.
     */
    private void discoverAndRegisterHandler(Object bean, Class<?> beanClass) {
        logger.debug("Processando handler candidate: {}", beanClass.getName());

        // Analisa interfaces genéricas
        Type[] genericInterfaces = beanClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            registerHandlerFromType(bean, genericInterface);
        }

        // Analisa superclasse genérica
        Type genericSuperclass = beanClass.getGenericSuperclass();
        if (genericSuperclass != null && genericSuperclass instanceof ParameterizedType) {
            registerHandlerFromType(bean, genericSuperclass);
        }

        // Log de sucesso
        logger.info("Handler registrado: {} -> {}",
                extractRequestTypeFromHandler(beanClass), beanClass.getSimpleName());
    }

    /**
     * Registra o handler a partir do tipo genérico.
     *
     * @param bean Instância do bean.
     * @param type Tipo genérico.
     */
    private void registerHandlerFromType(Object bean, Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();

            if (isHandlerInterfaceType(rawType)) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length >= 1 && typeArguments[0] instanceof Class<?> requestType) {
                    handlerRegistry.registerHandler(requestType, bean);
                    logger.debug("Handler registrado para request type: {}", requestType.getSimpleName());
                }
            }
        }
    }

    /**
     * Extrai o tipo de request do handler.
     *
     * @param handlerClass Classe do handler.
     * @return Nome do tipo de request.
     */
    private String extractRequestTypeFromHandler(Class<?> handlerClass) {
        try {
            // Tenta extrair o tipo de request dos generics
            Type[] genericInterfaces = handlerClass.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType parameterizedType) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        return typeArguments[0].getTypeName();
                    }
                }
            }
            return "Unknown";
        } catch (Exception e) {
            logger.warn("Não foi possível extrair request type do handler: {}", e.getMessage());
            return "Error";
        }
    }
}