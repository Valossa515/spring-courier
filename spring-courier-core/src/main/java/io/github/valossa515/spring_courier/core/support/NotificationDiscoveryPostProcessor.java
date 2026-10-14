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
                        targetClass.getName(),
                        e.getMessage(),
                        e
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
        String reason = null;

        for (Type genericInterface : beanClass.getGenericInterfaces()) {
            if (!isNotificationHandlerParameterizedInterface(genericInterface)) {
                continue;
            }

            ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
            Type notificationArg = parameterizedType.getActualTypeArguments()[0];

            String failure = tryRegisterConcreteNotification(bean, notificationArg);
            if (failure == null) {
                registered = true;
            } else if (reason == null) {
                reason = failure;
            }
        }

        if (!registered) {
            // Said in full because the consequence is silence, not an error:
            // publish() finds no handler, logs and returns, so the event is
            // dropped with nothing tying it back to this bean.
            logger.warn(
                    "Notification handler discovered but NOT registered: {}. {} Notifications it"
                            + " was meant to handle will be dropped silently when published.",
                    beanClass.getSimpleName(),
                    reason != null ? reason
                            : "Its NotificationHandler interface declares no notification type"
                                    + " argument, so the event type cannot be determined."
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

    /**
     * Registers the handler, or returns the reason it could not be registered.
     *
     * @return {@code null} on success, otherwise a sentence explaining why
     */
    private String tryRegisterConcreteNotification(Object bean, Type notificationArg) {
        if (notificationArg instanceof TypeVariable<?> typeVariable) {
            return "Its notification type is still the type variable '" + typeVariable.getName()
                    + "'; a handler bean must fix it to a concrete notification class.";
        }

        if (notificationArg instanceof ParameterizedType parameterizedType) {
            return "Its notification type " + parameterizedType.getTypeName() + " is generic, and"
                    + " type arguments are erased at runtime; use a concrete notification class.";
        }

        if (!(notificationArg instanceof Class<?> notificationType)) {
            return "Its notification type " + notificationArg.getTypeName()
                    + " could not be resolved to a class.";
        }

        if (!isConcrete(notificationType)) {
            // Dispatch is an exact-class lookup, so a handler keyed on an
            // interface or abstract class would never match a published event.
            return "Its notification type " + notificationType.getSimpleName() + " is "
                    + (notificationType.isInterface() ? "an interface" : "abstract")
                    + ", and notifications are dispatched by exact class; handlers must name the"
                    + " concrete notification type.";
        }

        notificationRegistry.registerHandler(notificationType, bean);
        logger.info(
                "Notification handler registered: {} -> {}",
                notificationType.getSimpleName(),
                bean.getClass().getSimpleName()
        );
        return null;
    }

    private boolean isConcrete(Class<?> type) {
        return !type.isInterface() && !Modifier.isAbstract(type.getModifiers());
    }
}
