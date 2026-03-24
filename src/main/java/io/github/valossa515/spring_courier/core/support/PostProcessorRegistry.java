package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequestPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry for {@link IRequestPostProcessor} instances.
 */
public class PostProcessorRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PostProcessorRegistry.class);
    private final Map<Class<?>, List<IRequestPostProcessor<?, ?>>> processors =
            new ConcurrentHashMap<>();
    private final List<IRequestPostProcessor<?, ?>> globalProcessors =
            new CopyOnWriteArrayList<>();
    private volatile boolean frozen = false;

    public void register(Class<?> requestType, IRequestPostProcessor<?, ?> processor) {
        Objects.requireNonNull(requestType, "requestType must not be null");
        Objects.requireNonNull(processor, "processor must not be null");
        if (frozen) {
            throw new IllegalStateException(
                    "PostProcessorRegistry is frozen; cannot register processor for: "
                            + requestType.getSimpleName());
        }
        if (requestType.isInterface()) {
            globalProcessors.add(processor);
            logger.info("Global post-processor registered: {}",
                    processor.getClass().getSimpleName());
        } else {
            processors.computeIfAbsent(requestType,
                    k -> new CopyOnWriteArrayList<>()).add(processor);
            logger.info("Post-processor registered for {}: {}",
                    requestType.getSimpleName(),
                    processor.getClass().getSimpleName());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<IRequestPostProcessor<?, ?>> getProcessors(
            Class<?> requestType) {
        List<IRequestPostProcessor<?, ?>> result =
                new java.util.ArrayList<>();
        result.addAll(globalProcessors);
        List<IRequestPostProcessor<?, ?>> specific =
                processors.get(requestType);
        if (specific != null) {
            result.addAll(specific);
        }
        return Collections.unmodifiableList(result);
    }

    public void freeze() {
        this.frozen = true;
        int total = globalProcessors.size()
                + processors.values().stream().mapToInt(List::size).sum();
        logger.info("PostProcessorRegistry frozen with {} post-processors", total);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int getCount() {
        return globalProcessors.size()
                + processors.values().stream().mapToInt(List::size).sum();
    }
}
