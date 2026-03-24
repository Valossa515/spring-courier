package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import io.github.valossa515.spring_courier.core.interfaces.IRequestPostProcessor;
import io.github.valossa515.spring_courier.core.interfaces.IRequestPreProcessor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * {@link BeanPostProcessor} that discovers {@link IRequestPreProcessor},
 * {@link IRequestPostProcessor}, and {@link IRequestExceptionHandler} beans
 * and registers them in their respective registries during context startup.
 */
public class ProcessorDiscoveryPostProcessor implements BeanPostProcessor {

    private static final Logger logger =
            LoggerFactory.getLogger(ProcessorDiscoveryPostProcessor.class);

    private final PreProcessorRegistry preProcessorRegistry;
    private final PostProcessorRegistry postProcessorRegistry;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;

    public ProcessorDiscoveryPostProcessor(
            PreProcessorRegistry preProcessorRegistry,
            PostProcessorRegistry postProcessorRegistry,
            ExceptionHandlerRegistry exceptionHandlerRegistry) {
        this.preProcessorRegistry = preProcessorRegistry;
        this.postProcessorRegistry = postProcessorRegistry;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(
            @NotNull Object bean, @NotNull String beanName) throws BeansException {

        Class<?> targetClass = Objects.requireNonNullElseGet(
                AopUtils.getTargetClass(bean), bean::getClass);

        if (bean instanceof IRequestPreProcessor<?> processor) {
            Class<?> requestType = extractFirstTypeArgument(
                    targetClass, IRequestPreProcessor.class);
            if (requestType != null) {
                preProcessorRegistry.register(requestType, processor);
            }
        }

        if (bean instanceof IRequestPostProcessor<?, ?> processor) {
            Class<?> requestType = extractFirstTypeArgument(
                    targetClass, IRequestPostProcessor.class);
            if (requestType != null) {
                postProcessorRegistry.register(requestType, processor);
            }
        }

        if (bean instanceof IRequestExceptionHandler<?, ?, ?> handler) {
            Class<?> requestType = extractFirstTypeArgument(
                    targetClass, IRequestExceptionHandler.class);
            if (requestType != null) {
                exceptionHandlerRegistry.register(requestType, handler);
            }
        }

        return bean;
    }

    private Class<?> extractFirstTypeArgument(
            Class<?> concreteClass, Class<?> targetInterface) {
        for (Type type : concreteClass.getGenericInterfaces()) {
            if (type instanceof ParameterizedType pt
                    && targetInterface.isAssignableFrom(
                    (Class<?>) pt.getRawType())) {
                Type arg = pt.getActualTypeArguments()[0];
                if (arg instanceof Class<?> cls) {
                    return cls;
                }
                if (arg instanceof ParameterizedType argPt
                        && argPt.getRawType() instanceof Class<?> raw) {
                    return raw;
                }
            }
        }
        // Check superclass
        Type superType = concreteClass.getGenericSuperclass();
        if (superType instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            if (raw instanceof Class<?> rawClass
                    && targetInterface.isAssignableFrom(rawClass)) {
                Type arg = pt.getActualTypeArguments()[0];
                if (arg instanceof Class<?> cls) {
                    return cls;
                }
            }
        }
        if (concreteClass.getSuperclass() != null
                && concreteClass.getSuperclass() != Object.class) {
            return extractFirstTypeArgument(
                    concreteClass.getSuperclass(), targetInterface);
        }
        logger.warn("Could not resolve request type for {} implementing {}",
                concreteClass.getSimpleName(), targetInterface.getSimpleName());
        return null;
    }
}
