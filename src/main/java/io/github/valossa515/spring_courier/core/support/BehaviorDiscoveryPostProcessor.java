package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.pipelines.BehaviorTypeResolver;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Objects;

/**
 * {@link BeanPostProcessor} that discovers {@link PipelineBehavior} beans and
 * registers them in the {@link PipelineRegistry} during context startup.
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
            Class<?> targetClass = Objects.requireNonNullElseGet(
                    AopUtils.getTargetClass(bean),
                    bean::getClass
            );

            logger.debug("Pipeline behavior detected: {}", targetClass.getSimpleName());
            registerPipelineBehavior(behavior, targetClass, beanName);
        }

        return bean;
    }

    private void registerPipelineBehavior(PipelineBehavior<?, ?> behavior,
                                          Class<?> behaviorClass, String beanName) {
        Class<?> requestType = extractRequestTypeFromBehavior(behaviorClass);

        if (requestType != null && !requestType.isInterface()) {
            pipelineRegistry.registerBehavior(requestType, behavior);
            logger.info("Pipeline behavior registered: {} -> {} (bean '{}')",
                    requestType.getSimpleName(), behaviorClass.getName(), beanName);
        } else if (requestType != null) {
            pipelineRegistry.registerGlobalBehavior(behavior, requestType);
            logger.info("Global pipeline behavior registered: {} (matches {}) -> {} (bean '{}')",
                    behaviorClass.getSimpleName(), requestType.getSimpleName(),
                    behaviorClass.getName(), beanName);
        } else {
            pipelineRegistry.registerGlobalBehavior(behavior, null);
            logger.warn("Could not resolve request type for pipeline behavior '{}' [{}] (bean '{}'). "
                            + "Registering as global (will not match any request). "
                            + "Create a concrete subclass to preserve generic type information.",
                    behaviorClass.getSimpleName(), behaviorClass.getName(), beanName);
        }
    }

    private Class<?> extractRequestTypeFromBehavior(Class<?> behaviorClass) {
        return BehaviorTypeResolver.extractRequestType(behaviorClass);
    }
}