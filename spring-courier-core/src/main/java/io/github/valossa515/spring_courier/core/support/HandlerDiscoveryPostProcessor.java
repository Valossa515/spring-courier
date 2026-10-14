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
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link BeanPostProcessor} that detects handler beans and registers them in
 * the {@link HandlerRegistry} so they can participate in the CQRS pipeline.
 */
public class HandlerDiscoveryPostProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(HandlerDiscoveryPostProcessor.class);
    private final Map<Class<?>, Boolean> handlerInterfaceCache = new ConcurrentHashMap<>();
    private final HandlerRegistry handlerRegistry;
    private final ApplicationContext applicationContext;

    public HandlerDiscoveryPostProcessor(HandlerRegistry handlerRegistry, ApplicationContext applicationContext) {
        this.handlerRegistry = handlerRegistry;
        this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext");
    }

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        logger.trace("Context {} inspecting bean {}", applicationContext.getId(), beanName);
        Class<?> targetClass = Objects.requireNonNullElseGet(AopUtils.getTargetClass(bean), bean::getClass);

        if (shouldProcessBean(targetClass)) {
            try {
                discoverAndRegisterHandler(bean, targetClass);
            } catch (Exception e) {
                logger.error("Error while processing handler {}: {}", targetClass.getName(), e.getMessage(), e);
            }
        }

        return bean;
    }

    /**
     * Determines whether the bean should be treated as a handler candidate.
     */
    private boolean shouldProcessBean(@NotNull Class<?> beanClass) {
        return beanClass.isAnnotationPresent(ExposeHandler.class)
                || isHandlerInterface(beanClass);
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
            logger.debug("Handler interface detected: {}", interfaceType.getSimpleName());
            return true;
        }

        for (Type genericInterface : interfaceType.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class<?> rawClass &&
                        io.github.valossa515.spring_courier.core.interfaces.IRequest.class.isAssignableFrom(rawClass)) {
                    logger.debug("Custom interface detected as IRequest: {}", interfaceType.getSimpleName());
                    return true;
                }
            }
        }

        return hasValidHandleMethod(interfaceType);
    }

    private boolean hasValidHandleMethod(@NotNull Class<?> interfaceType) {
        try {
            return Arrays.stream(interfaceType.getMethods())
                    .anyMatch(m -> ("handle".equals(m.getName()) || "execute".equals(m.getName()))
                            && m.getParameterCount() == 1
                            && m.getReturnType() != Void.TYPE
                            && io.github.valossa515.spring_courier.core.interfaces.IRequest.class
                                    .isAssignableFrom(m.getParameterTypes()[0]));
        } catch (SecurityException e) {
            logger.warn(
                    "Security error while analyzing interface {}: {}",
                    interfaceType.getSimpleName(),
                    e.getMessage()
            );
            return false;
        }
    }

    /**
     * Registers handlers based on their declared generic types.
     */
    private void discoverAndRegisterHandler(Object bean, @NotNull Class<?> beanClass) {
        logger.debug("Processing handler candidate: {}", beanClass.getName());

        boolean registered = false;
        Type[] genericInterfaces = beanClass.getGenericInterfaces();

        for (Type genericInterface : genericInterfaces) {
            registered |= registerHandlerFromType(bean, genericInterface);
        }

        Type genericSuperclass = beanClass.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType parameterizedType) {
            registered |= registerHandlerFromType(bean, parameterizedType);
        }

        String requestType = extractRequestTypeFromHandler(beanClass);
        if (registered) {
            logger.info("Handler registered: {} -> {}", requestType, beanClass.getSimpleName());
        } else {
            logger.warn("Handler discovered but NOT registered: {}. {} Requests of this type will"
                            + " fail with HandlerNotFoundException when dispatched.",
                    beanClass.getSimpleName(), describeWhyNotRegistered(beanClass));
        }
    }

    /**
     * Explains why a handler candidate could not be registered.
     *
     * <p>Worth the effort because the consequence surfaces far away: the bean
     * starts fine, and the failure appears later as a
     * {@code HandlerNotFoundException} on the first dispatch, with nothing
     * pointing back to this warning. The message names the actual cause so the
     * startup log is enough to fix it.
     */
    private String describeWhyNotRegistered(Class<?> beanClass) {
        Type requestArg = firstHandlerTypeArgument(beanClass);

        if (requestArg == null) {
            return "Its handler interface declares no request type argument, so the request type"
                    + " cannot be determined; parameterize it with the request class"
                    + " (e.g. CommandHandler<CreateOrderCommand, Order>).";
        }
        if (requestArg instanceof TypeVariable<?> typeVariable) {
            return "Its request type is still the type variable '" + typeVariable.getName()
                    + "'; a handler bean must fix it to a concrete request class.";
        }
        if (requestArg instanceof ParameterizedType parameterizedType) {
            return "Its request type " + parameterizedType.getTypeName() + " is generic, and type"
                    + " arguments are erased at runtime; use a concrete request class instead.";
        }
        return "Its request type " + requestArg.getTypeName() + " could not be resolved to a class.";
    }

    /**
     * Returns the request type argument declared on the bean's handler
     * interface (or generic superclass), or {@code null} when there is none.
     */
    private Type firstHandlerTypeArgument(Class<?> beanClass) {
        Type[] candidates = Arrays.copyOf(beanClass.getGenericInterfaces(),
                beanClass.getGenericInterfaces().length + 1);
        candidates[candidates.length - 1] = beanClass.getGenericSuperclass();

        for (Type candidate : candidates) {
            if (candidate instanceof ParameterizedType parameterizedType
                    && parameterizedType.getRawType() instanceof Class<?> rawClass
                    && isHandlerInterfaceType(rawClass)
                    && parameterizedType.getActualTypeArguments().length >= 1) {
                return parameterizedType.getActualTypeArguments()[0];
            }
        }
        return null;
    }

    private boolean registerHandlerFromType(Object bean, Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class<?> rawClass && isHandlerInterfaceType(rawClass)) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length >= 1 && typeArguments[0] instanceof Class<?> requestType) {
                    handlerRegistry.registerHandler(requestType, bean);
                    logger.debug("Handler registered for request type: {}", requestType.getSimpleName());
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

            Type superType = handlerClass.getGenericSuperclass();
            if (superType instanceof ParameterizedType parameterizedType) {
                Type[] args = parameterizedType.getActualTypeArguments();
                if (args.length > 0) {
                    return args[0].getTypeName();
                }
            }
        } catch (Exception e) {
            logger.warn("Could not extract request type from handler: {}", e.getMessage());
        }
        return "Unknown";
    }
}
