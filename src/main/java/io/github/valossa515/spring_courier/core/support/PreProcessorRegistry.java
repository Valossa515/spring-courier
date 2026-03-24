package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.IRequestPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry for {@link IRequestPreProcessor} instances.
 * Global processors are stored with their resolved request type so
 * that only type-compatible processors are returned at lookup time.
 */
public class PreProcessorRegistry {
    private static final Logger logger =
            LoggerFactory.getLogger(PreProcessorRegistry.class);
    private final Map<Class<?>, List<IRequestPreProcessor<?>>> processors =
            new ConcurrentHashMap<>();
    private final List<GlobalEntry> globalProcessors =
            new CopyOnWriteArrayList<>();
    private volatile boolean frozen = false;

    record GlobalEntry(IRequestPreProcessor<?> processor,
                       Class<?> requestType) {
    }

    public void register(Class<?> requestType,
                         IRequestPreProcessor<?> processor) {
        Objects.requireNonNull(requestType, "requestType must not be null");
        Objects.requireNonNull(processor, "processor must not be null");
        if (frozen) {
            throw new IllegalStateException(
                    "PreProcessorRegistry is frozen; cannot register "
                            + "processor for: "
                            + requestType.getSimpleName());
        }
        if (requestType.isInterface()) {
            globalProcessors.add(new GlobalEntry(processor, requestType));
            globalProcessors.sort(Comparator.comparingInt(
                    e -> getProcessorOrder(e.processor())));
            logger.info("Global pre-processor registered: {} (matches {})",
                    processor.getClass().getSimpleName(),
                    requestType.getSimpleName());
        } else {
            List<IRequestPreProcessor<?>> list =
                    processors.computeIfAbsent(requestType,
                            k -> new CopyOnWriteArrayList<>());
            list.add(processor);
            list.sort(Comparator.comparingInt(this::getProcessorOrder));
            logger.info("Pre-processor registered for {}: {}",
                    requestType.getSimpleName(),
                    processor.getClass().getSimpleName());
        }
    }

    public List<IRequestPreProcessor<?>> getProcessors(
            Class<?> requestType) {
        List<IRequestPreProcessor<?>> result = new ArrayList<>();
        for (GlobalEntry entry : globalProcessors) {
            if (entry.requestType() != null
                    && entry.requestType().isAssignableFrom(requestType)) {
                result.add(entry.processor());
            }
        }
        List<IRequestPreProcessor<?>> specific =
                processors.get(requestType);
        if (specific != null) {
            result.addAll(specific);
        }
        result.sort(Comparator.comparingInt(this::getProcessorOrder));
        return Collections.unmodifiableList(result);
    }

    public void freeze() {
        this.frozen = true;
        int total = globalProcessors.size()
                + processors.values().stream()
                .mapToInt(List::size).sum();
        logger.info("PreProcessorRegistry frozen with {} pre-processors",
                total);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int getCount() {
        return globalProcessors.size()
                + processors.values().stream()
                .mapToInt(List::size).sum();
    }

    /**
     * Resolves the execution order for a pre-processor based on
     * {@link Order} annotation or {@link Ordered} interface.
     */
    private int getProcessorOrder(IRequestPreProcessor<?> processor) {
        Class<?> clazz = processor.getClass();
        Order orderAnnotation = clazz.getAnnotation(Order.class);
        if (orderAnnotation != null) {
            return orderAnnotation.value();
        }
        if (processor instanceof Ordered ordered) {
            return ordered.getOrder();
        }
        return Ordered.LOWEST_PRECEDENCE;
    }
}
