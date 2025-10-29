package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.annotations.ExposeHandler;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BeanPostProcessor responsável por descobrir e registrar handlers de comandos e queries
 * durante a inicialização dos beans no contexto Spring.
 * <p>
 * Compatível com beans proxied pelo Spring (AOP/CGLIB) e suporta handlers anotados ou
 * baseados em interfaces conhecidas (CommandHandler, QueryHandler).
 * </p>
 *
 * @author
 *     Felipe Martins (Valossa515)
 */
public class HandlerDiscoveryPostProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(HandlerDiscoveryPostProcessor.class);
    private final Map<Class<?>, Boolean> handlerInterfaceCache = new ConcurrentHashMap<>();

    private final HandlerRegistry handlerRegistry;

    public HandlerDiscoveryPostProcessor(HandlerRegistry handlerRegistry, ApplicationContext applicationContext) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        if (targetClass == null) {
            logger.warn("Não foi possível determinar a classe alvo do bean: {}", beanName);
            return bean;
        }

        if (shouldProcessBean(targetClass)) {
            try {
                discoverAndRegisterHandler(bean, targetClass);
            } catch (Exception e) {
                logger.error("Erro ao processar handler {}: {}", targetClass.getName(), e.getMessage(), e);
            }
        }

        return bean;
    }

    private boolean shouldProcessBean(@NotNull Class<?> beanClass) {
        return beanClass.isAnnotationPresent(ExposeHandler.class) || isHandlerInterface(beanClass);
    }

    private boolean isHandlerInterface(@NotNull Class<?> beanClass) {
        return Arrays.stream(beanClass.getInterfaces()).anyMatch(this::isHandlerInterfaceType)
                || (beanClass.getSuperclass() != null && isHandlerInterfaceType(beanClass.getSuperclass()));
    }

    private boolean isHandlerInterfaceType(Class<?> interfaceType) {
        return handlerInterfaceCache.computeIfAbsent(interfaceType, this::checkIfHandlerInterface);
    }

    private boolean checkIfHandlerInterface(Class<?> interfaceType) {
        if (CommandHandler.class.isAssignableFrom(interfaceType) ||
                QueryHandler.class.isAssignableFrom(interfaceType)) {
            logger.debug("Interface identificada como handler padrão: {}", interfaceType.getSimpleName());
            return true;
        }

        return hasValidHandleMethod(interfaceType);
    }

    private boolean hasValidHandleMethod(@NotNull Class<?> interfaceType) {
        try {
            return Arrays.stream(interfaceType.getMethods())
                    .anyMatch(m -> "handle".equals(m.getName())
                            && m.getParameterCount() == 1
                            && m.getReturnType() != Void.TYPE);
        } catch (SecurityException e) {
            logger.warn("Erro de segurança ao analisar interface {}: {}", interfaceType.getSimpleName(), e.getMessage());
            return false;
        }
    }

    private void discoverAndRegisterHandler(Object bean, @NotNull Class<?> beanClass) {
        logger.debug("Processando handler candidate: {}", beanClass.getName());

        boolean registered = false;
        Type[] genericInterfaces = beanClass.getGenericInterfaces();

        for (Type genericInterface : genericInterfaces) {
            registered |= registerHandlerFromType(bean, genericInterface);
        }

        if (beanClass.getGenericSuperclass() instanceof ParameterizedType parameterizedType) {
            registered |= registerHandlerFromType(bean, parameterizedType);
        }

        String requestType = extractRequestTypeFromHandler(beanClass);
        if (registered) {
            logger.info("✅ Handler registrado: {} -> {}", requestType, beanClass.getSimpleName());
        } else {
            logger.warn("⚠️ Handler descoberto mas não registrado (tipo não resolvido): {}", beanClass.getSimpleName());
        }
    }

    private boolean registerHandlerFromType(Object bean, Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> rawClass && isHandlerInterfaceType(rawClass)) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length >= 1 && typeArguments[0] instanceof Class<?> requestType) {
                    handlerRegistry.registerHandler(requestType, bean);
                    logger.debug("Handler registrado para request type: {}", requestType.getSimpleName());
                    return true;
                }
            }
        }
        return false;
    }

    private String extractRequestTypeFromHandler(Class<?> handlerClass) {
        try {
            for (Type genericInterface : handlerClass.getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType parameterizedType) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        return typeArguments[0].getTypeName();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Não foi possível extrair request type do handler: {}", e.getMessage());
        }
        return "Unknown";
    }
}