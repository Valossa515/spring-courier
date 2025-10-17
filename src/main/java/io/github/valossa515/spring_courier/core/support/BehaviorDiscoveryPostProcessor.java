package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class BehaviorDiscoveryPostProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(BehaviorDiscoveryPostProcessor.class);

    private final PipelineRegistry pipelineRegistry;

    public BehaviorDiscoveryPostProcessor(PipelineRegistry pipelineRegistry) {
        this.pipelineRegistry = pipelineRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        if (bean instanceof PipelineBehavior<?, ?> behavior) {
            logger.debug("Pipeline behavior bean detected: {}", beanName);
            registerPipelineBehavior(behavior);
        }

        return bean;
    }

    private void registerPipelineBehavior(PipelineBehavior<?, ?> behavior) {
        Class<?> behaviorClass = behavior.getClass();
        Class<?> requestType = extractRequestTypeFromBehavior(behaviorClass);

        if (requestType != null) {
            pipelineRegistry.registerBehavior(requestType, behavior);
            logger.info("Registered pipeline behavior: {} -> {}",
                    requestType.getSimpleName(), behaviorClass.getSimpleName());
        } else {
            logger.warn("Could not determine request type for behavior: {}", behaviorClass.getSimpleName());
        }
    }

    private Class<?> extractRequestTypeFromBehavior(Class<?> behaviorClass) {
        // Analisa interfaces genéricas
        Type[] genericInterfaces = behaviorClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            Class<?> requestType = extractRequestTypeFromParameterizedType(genericInterface);
            if (requestType != null) {
                return requestType;
            }
        }

        // Analisa superclasse genérica
        Type genericSuperclass = behaviorClass.getGenericSuperclass();
        return extractRequestTypeFromParameterizedType(genericSuperclass);
    }

    private Class<?> extractRequestTypeFromParameterizedType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (PipelineBehavior.class.isAssignableFrom(rawType)) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?> requestType) {
                    return requestType;
                }
            }
        }
        return null;
    }
}
