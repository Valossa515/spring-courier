package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry for {@link PreProcessor}, {@link PostProcessor},
 * and {@link IRequestExceptionHandler} instances discovered from the
 * Spring context.
 *
 * <p>Like other Courier registries, this registry supports freezing
 * after context initialization to prevent runtime modifications.
 */
public class ProcessorRegistry {

    private static final Logger logger =
            LoggerFactory.getLogger(ProcessorRegistry.class);

    private final List<PreProcessor<?>> preProcessors =
            new CopyOnWriteArrayList<>();
    private final List<PostProcessor<?, ?>> postProcessors =
            new CopyOnWriteArrayList<>();
    private final List<IRequestExceptionHandler<?, ?>> exceptionHandlers =
            new CopyOnWriteArrayList<>();
    private volatile boolean frozen = false;

    public void registerPreProcessor(PreProcessor<?> processor) {
        Objects.requireNonNull(processor, "processor must not be null");
        checkNotFrozen("PreProcessor");
        preProcessors.add(processor);
    }

    public void registerPostProcessor(PostProcessor<?, ?> processor) {
        Objects.requireNonNull(processor, "processor must not be null");
        checkNotFrozen("PostProcessor");
        postProcessors.add(processor);
    }

    public void registerExceptionHandler(
            IRequestExceptionHandler<?, ?> handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        checkNotFrozen("IRequestExceptionHandler");
        exceptionHandlers.add(handler);
    }

    public List<PreProcessor<?>> getPreProcessors() {
        return Collections.unmodifiableList(preProcessors);
    }

    public List<PostProcessor<?, ?>> getPostProcessors() {
        return Collections.unmodifiableList(postProcessors);
    }

    public List<IRequestExceptionHandler<?, ?>> getExceptionHandlers() {
        return Collections.unmodifiableList(exceptionHandlers);
    }

    public void freeze() {
        this.frozen = true;
        logger.info("ProcessorRegistry frozen with {} pre-processors, "
                        + "{} post-processors, {} exception handlers",
                preProcessors.size(), postProcessors.size(),
                exceptionHandlers.size());
    }

    public boolean isFrozen() {
        return frozen;
    }

    private void checkNotFrozen(String type) {
        if (frozen) {
            throw new IllegalStateException(
                    "ProcessorRegistry is frozen; cannot register "
                            + type);
        }
    }
}
