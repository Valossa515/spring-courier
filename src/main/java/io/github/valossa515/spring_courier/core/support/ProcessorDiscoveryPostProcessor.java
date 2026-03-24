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
import org.springframework.core.ResolvableType;

import java.util.Objects;

/**
 * {@link BeanPostProcessor} that discovers {@link IRequestPreProcessor},
 * {@link IRequestPostProcessor}, and {@link IRequestExceptionHandler} beans
 * and registers them in their respective registries during context startup.
 *
 * <p>Type arguments are resolved using Spring's {@link ResolvableType},
 * which correctly handles CGLIB proxies, JDK proxies, and
 * {@code TypeVariable} parameters.
 */
public class ProcessorDiscoveryPostProcessor
        implements BeanPostProcessor {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    ProcessorDiscoveryPostProcessor.class);

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
            @NotNull Object bean,
            @NotNull String beanName) throws BeansException {

        Class<?> targetClass = Objects.requireNonNullElseGet(
                AopUtils.getTargetClass(bean), bean::getClass);

        if (bean instanceof IRequestPreProcessor<?> processor) {
            Class<?> requestType = resolveTypeArg(
                    targetClass, IRequestPreProcessor.class, 0);
            if (requestType != null) {
                preProcessorRegistry.register(
                        requestType, processor);
            }
        }

        if (bean instanceof IRequestPostProcessor<?, ?> processor) {
            Class<?> requestType = resolveTypeArg(
                    targetClass, IRequestPostProcessor.class, 0);
            if (requestType != null) {
                postProcessorRegistry.register(
                        requestType, processor);
            }
        }

        if (bean instanceof IRequestExceptionHandler<?, ?, ?>
                handler) {
            Class<?> requestType = resolveTypeArg(
                    targetClass,
                    IRequestExceptionHandler.class, 0);
            Class<?> exceptionRaw = resolveTypeArg(
                    targetClass,
                    IRequestExceptionHandler.class, 2);
            @SuppressWarnings("unchecked")
            Class<? extends Exception> exType =
                    exceptionRaw != null
                            && Exception.class
                            .isAssignableFrom(exceptionRaw)
                            ? (Class<? extends Exception>)
                            exceptionRaw
                            : Exception.class;
            if (requestType != null) {
                exceptionHandlerRegistry.register(
                        requestType, handler, exType);
            }
        }

        return bean;
    }

    /**
     * Resolves a specific type argument from a concrete class
     * implementing the given interface using Spring's
     * {@link ResolvableType}. Returns {@code null} when the type
     * argument cannot be resolved to a concrete class.
     *
     * @param concreteClass   the (possibly proxied) bean class
     * @param targetInterface the generic interface to resolve against
     * @param argIndex        zero-based index of the type argument
     * @return the resolved {@link Class}, or {@code null}
     */
    private Class<?> resolveTypeArg(Class<?> concreteClass,
                                    Class<?> targetInterface,
                                    int argIndex) {
        ResolvableType resolvable = ResolvableType.forClass(
                concreteClass).as(targetInterface);
        if (resolvable == ResolvableType.NONE) {
            logger.warn("Could not resolve {} as {} via "
                            + "ResolvableType",
                    concreteClass.getSimpleName(),
                    targetInterface.getSimpleName());
            return null;
        }
        ResolvableType generic =
                resolvable.getGeneric(argIndex);
        Class<?> resolved = generic.resolve();
        if (resolved == null) {
            logger.warn("Could not resolve type argument {} "
                            + "for {} implementing {}",
                    argIndex,
                    concreteClass.getSimpleName(),
                    targetInterface.getSimpleName());
        }
        return resolved;
    }
}
