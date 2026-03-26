package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import io.github.valossa515.spring_courier.core.pipelines.PostProcessor;
import io.github.valossa515.spring_courier.core.pipelines.PreProcessor;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor} that auto-discovers {@link PreProcessor},
 * {@link PostProcessor}, and {@link IRequestExceptionHandler} beans
 * and registers them in the {@link ProcessorRegistry}.
 */
public class ProcessorDiscoveryPostProcessor implements BeanPostProcessor {

    private static final Logger logger =
            LoggerFactory.getLogger(ProcessorDiscoveryPostProcessor.class);
    private final ProcessorRegistry processorRegistry;

    public ProcessorDiscoveryPostProcessor(
            ProcessorRegistry processorRegistry) {
        this.processorRegistry = processorRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean,
            String beanName) throws BeansException {
        if (bean instanceof PreProcessor<?> pre) {
            processorRegistry.registerPreProcessor(pre);
            logger.debug("Registered PreProcessor: {}", beanName);
        }
        if (bean instanceof PostProcessor<?, ?> post) {
            processorRegistry.registerPostProcessor(post);
            logger.debug("Registered PostProcessor: {}", beanName);
        }
        if (bean instanceof IRequestExceptionHandler<?, ?> handler) {
            processorRegistry.registerExceptionHandler(handler);
            logger.debug("Registered IRequestExceptionHandler: {}",
                    beanName);
        }
        return bean;
    }
}
