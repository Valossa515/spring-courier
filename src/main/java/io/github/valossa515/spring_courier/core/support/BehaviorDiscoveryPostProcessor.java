package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * BeanPostProcessor responsável por descobrir e registrar comportamentos (PipelineBehaviors)
 * durante a inicialização do contexto Spring.
 *
 * Agora compatível com beans proxied pelo Spring (CGLIB/AOP),
 * garantindo que o tipo de requisição genérico seja resolvido corretamente.
 */
public class BehaviorDiscoveryPostProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(BehaviorDiscoveryPostProcessor.class);

    private final PipelineRegistry pipelineRegistry;

    public BehaviorDiscoveryPostProcessor(PipelineRegistry pipelineRegistry) {
        this.pipelineRegistry = pipelineRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        if (bean instanceof PipelineBehavior<?, ?> behavior) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            if (targetClass == null) {
                logger.warn("Não foi possível determinar a classe alvo para o behavior: {}", beanName);
                return bean;
            }

            logger.debug("🔍 Pipeline behavior detectado: {}", targetClass.getSimpleName());
            registerPipelineBehavior(behavior, targetClass);
        }

        return bean;
    }

    private void registerPipelineBehavior(PipelineBehavior<?, ?> behavior, Class<?> behaviorClass) {
        Class<?> requestType = extractRequestTypeFromBehavior(behaviorClass);

        if (requestType != null) {
            pipelineRegistry.registerBehavior(requestType, behavior);
            logger.info("✅ Pipeline behavior registrado: {} -> {}",
                    requestType.getSimpleName(), behaviorClass.getSimpleName());
        } else {
            logger.warn("⚠️ Não foi possível determinar o tipo de request para behavior: {}",
                    behaviorClass.getSimpleName());
        }
    }

    private Class<?> extractRequestTypeFromBehavior(Class<?> behaviorClass) {
        // Verifica interfaces genéricas da classe alvo
        Type[] genericInterfaces = behaviorClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            Class<?> requestType = extractRequestTypeFromParameterizedType(genericInterface);
            if (requestType != null) {
                return requestType;
            }
        }

        // Verifica superclasse genérica
        Type genericSuperclass = behaviorClass.getGenericSuperclass();
        return extractRequestTypeFromParameterizedType(genericSuperclass);
    }

    private Class<?> extractRequestTypeFromParameterizedType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class<?> rawClass
                    && PipelineBehavior.class.isAssignableFrom(rawClass)) {

                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?> requestType) {
                    return requestType;
                }
            }
        }
        return null;
    }
}
