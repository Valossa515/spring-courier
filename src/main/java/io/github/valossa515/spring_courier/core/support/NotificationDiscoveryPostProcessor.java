package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * {@link BeanPostProcessor} that detects notification handler beans and registers 
 * them in the {@link NotificationRegistry} so they can receive published notifications.
 */
public class NotificationDiscoveryPostProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(NotificationDiscoveryPostProcessor.class);
    private final NotificationRegistry notificationRegistry;

    public NotificationDiscoveryPostProcessor(NotificationRegistry notificationRegistry, 
                                             ApplicationContext applicationContext) {
        this.notificationRegistry = notificationRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        Class<?> targetClass = Objects.requireNonNullElseGet(AopUtils.getTargetClass(bean), bean::getClass);

        if (isNotificationHandler(targetClass)) {
            try {
                discoverAndRegisterNotificationHandler(bean, targetClass);
            } catch (Exception e) {
                logger.error("Erro ao processar notification handler {}: {}",
                        targetClass.getName(), e.getMessage(), e);
            }
        }

        return bean;
    }

    /**
     * Determines whether the bean is a notification handler.
     */
    private boolean isNotificationHandler(@NotNull Class<?> beanClass) {
        for (Class<?> iface : beanClass.getInterfaces()) {
            if (NotificationHandler.class.isAssignableFrom(iface)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers the notification handler based on its declared generic type.
     */
    private void discoverAndRegisterNotificationHandler(Object bean, @NotNull Class<?> beanClass) {
        logger.debug("Processando notification handler candidate: {}", beanClass.getName());

        boolean registered = false;
        Type[] genericInterfaces = beanClass.getGenericInterfaces();

        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                Type rawType = parameterizedType.getRawType();

                if (rawType instanceof Class<?>) {
                    Class<?> rawClass = (Class<?>) rawType;
                    if (NotificationHandler.class.isAssignableFrom(rawClass)) {
                        Type[] typeArguments = parameterizedType.getActualTypeArguments();
                        if (typeArguments.length >= 1 && typeArguments[0] instanceof Class<?>) {
                            Class<?> notificationType = (Class<?>) typeArguments[0];
                            notificationRegistry.registerHandler(notificationType, bean);
                            logger.info("✅ Notification handler registrado: {} -> {}", 
                                    notificationType.getSimpleName(), beanClass.getSimpleName());
                            registered = true;
                        }
                    }
                }
            }
        }

        if (!registered) {
            logger.warn("⚠️ Notification handler descoberto mas não registrado (tipo não resolvido): {}", 
                    beanClass.getSimpleName());
        }
    }
}