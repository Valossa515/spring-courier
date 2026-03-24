package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequestPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry for {@link IRequestPreProcessor} instances.
 */
public class PreProcessorRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PreProcessorRegistry.class);
    private final Map<Class<?>, List<IRequestPreProcessor<?>>> processors =
            new ConcurrentHashMap<>();
    private final List<IRequestPreProcessor<?>> globalProcessors =
            new CopyOnWriteArrayList<>();
    private volatile boolean frozen = false;

    public void register(Class<?> requestType, IRequestPreProcessor<?> processor) {
        Objects.requireNonNull(requestType, "requestType must not be null");
        Objects.requireNonNull(processor, "processor must not be null");
        if (frozen) {
            throw new IllegalStateException(
                    "PreProcessorRegistry is frozen; cannot register processor for: "
                            + requestType.getSimpleName());
        }
        if (requestType.isInterface()) {
            globalProcessors.add(processor);
            logger.info("Global pre-processor registered: {}",
                    processor.getClass().getSimpleName());
        } else {
            processors.computeIfAbsent(requestType,
                    k -> new CopyOnWriteArrayList<>()).add(processor);
            logger.info("Pre-processor registered for {}: {}",
                    requestType.getSimpleName(),
                    processor.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<IRequestPreProcessor<T>> getProcessors(Class<T> requestType) {
        List<IRequestPreProcessor<T>> result = new java.util.ArrayList<>();
        for (IRequestPreProcessor<?> global : globalProcessors) {
            result.add((IRequestPreProcessor<T>) global);
        }
        List<IRequestPreProcessor<?>> specific = processors.get(requestType);
        if (specific != null) {
            for (IRequestPreProcessor<?> p : specific) {
                result.add((IRequestPreProcessor<T>) p);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public void freeze() {
        this.frozen = true;
        int total = globalProcessors.size()
                + processors.values().stream().mapToInt(List::size).sum();
        logger.info("PreProcessorRegistry frozen with {} pre-processors", total);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int getCount() {
        return globalProcessors.size()
                + processors.values().stream().mapToInt(List::size).sum();
    }
}
