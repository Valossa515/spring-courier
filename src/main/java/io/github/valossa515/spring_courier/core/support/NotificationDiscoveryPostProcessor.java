package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

/**
 * {@link BeanPostProcessor} that detects notification handler beans and registers
 * them in the {@link NotificationRegistry} so they can receive published notifications.
 */
public class NotificationDiscoveryPostProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(NotificationDiscoveryPostProcessor.class);
    private final NotificationRegistry notificationRegistry;
    private final ApplicationContext applicationContext;

    public NotificationDiscoveryPostProcessor(NotificationRegistry notificationRegistry,
                                              ApplicationContext applicationContext) {
        this.notificationRegistry = Objects.requireNonNull(notificationRegistry, "notificationRegistry");
        this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext");
    }

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        logger.trace("Context {} inspecting bean {}", applicationContext.getId(), beanName);
        Class<?> targetClass = Objects.requireNonNullElseGet(AopUtils.getTargetClass(bean), bean::getClass);

        if (isNotificationHandler(targetClass)) {
            try {
                discoverAndRegisterNotificationHandler(bean, targetClass);
            } catch (Exception e) {
                logger.error(
                        "Error while processing notification handler {}: {}",
                        targetClass.getSimpleName(),
                        e.getMessage()
                );
            }
        }

        return bean;
    }

    /**
     * Determines whether the bean is a notification handler.
     */
    private boolean isNotificationHandler(@NotNull Class<?> beanClass) {
        for (Class<?> implementedInterface : beanClass.getInterfaces()) {
            if (NotificationHandler.class.isAssignableFrom(implementedInterface)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers the notification handler based on its declared generic type.
     */
    private void discoverAndRegisterNotificationHandler(Object bean, @NotNull Class<?> beanClass) {
        logger.debug("Processing notification handler candidate: {}", beanClass.getName());

        boolean registered = false;

        for (Type genericInterface : beanClass.getGenericInterfaces()) {
            if (!isNotificationHandlerParameterizedInterface(genericInterface)) {
                continue;
            }

            ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
            Type notificationArg = parameterizedType.getActualTypeArguments()[0];

            if (tryRegisterConcreteNotification(bean, beanClass, notificationArg)) {
                registered = true;
            }
        }

        if (!registered) {
            logger.warn(
                    "Notification handler discovered but not registered (unresolved type): {}",
                    beanClass.getSimpleName()
            );
        }
    }

    private boolean isNotificationHandlerParameterizedInterface(Type genericInterface) {
        if (!(genericInterface instanceof ParameterizedType parameterizedType)) {
            return false;
        }
        if (!(parameterizedType.getRawType() instanceof Class<?> rawClass)) {
            return false;
        }
        return NotificationHandler.class.isAssignableFrom(rawClass)
                && parameterizedType.getActualTypeArguments().length >= 1;
    }

    private boolean tryRegisterConcreteNotification(Object bean, Class<?> beanClass, Type notificationArg) {
        if (notificationArg instanceof TypeVariable<?>) {
            logger.warn(
                    "Notification handler discovered but not registered (unresolved generic type): {}",
                    beanClass.getSimpleName()
            );
            return false;
        }

        if (!(notificationArg instanceof Class<?> notificationType)) {
            return false;
        }

        if (!isConcrete(notificationType)) {
            logger.warn(
                    "Notification handler discovered but not registered (non-concrete type): {}",
                    beanClass.getSimpleName()
            );
            return false;
        }

        notificationRegistry.registerHandler(notificationType, bean);
        logger.info(
                "Notification handler registered: {} -> {}",
                notificationType.getSimpleName(),
                beanClass.getSimpleName()
        );
        return true;
    }

    private boolean isConcrete(Class<?> type) {
        return !type.isInterface() && !Modifier.isAbstract(type.getModifiers());
    }
}
