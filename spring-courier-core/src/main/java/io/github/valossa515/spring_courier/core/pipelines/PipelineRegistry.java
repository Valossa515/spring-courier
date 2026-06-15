package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PipelineRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PipelineRegistry.class);
    private final Map<Class<?>, List<PipelineBehavior<?, ?>>> behaviorRegistry = new ConcurrentHashMap<>();
    private final List<GlobalBehaviorEntry> globalBehaviors = new CopyOnWriteArrayList<>();
    private volatile boolean frozen = false;

    record GlobalBehaviorEntry(PipelineBehavior<?, ?> behavior, Class<?> requestType) {
    }

    public void registerBehavior(Class<?> requestType, PipelineBehavior<?, ?> behavior) {
        Objects.requireNonNull(requestType, "requestType must not be null");
        Objects.requireNonNull(behavior, "behavior must not be null");
        if (frozen) {
            throw new IllegalStateException(
                    "PipelineRegistry is frozen; cannot register behavior for: "
                            + requestType.getSimpleName());
        }
        List<PipelineBehavior<?, ?>> list = behaviorRegistry.computeIfAbsent(
                requestType, k -> new CopyOnWriteArrayList<>());
        list.add(behavior);
        list.sort(Comparator.comparingInt(this::getBehaviorOrder));
    }

    public void registerGlobalBehavior(PipelineBehavior<?, ?> behavior) {
        registerGlobalBehavior(behavior, extractRequestTypeFromBehavior(behavior.getClass()));
    }

    public void registerGlobalBehavior(PipelineBehavior<?, ?> behavior, Class<?> resolvedRequestType) {
        Objects.requireNonNull(behavior, "behavior must not be null");
        if (frozen) {
            throw new IllegalStateException(
                    "PipelineRegistry is frozen; cannot register global behavior: "
                            + behavior.getClass().getSimpleName());
        }
        globalBehaviors.add(new GlobalBehaviorEntry(behavior, resolvedRequestType));
        globalBehaviors.sort(Comparator.comparingInt(e -> getBehaviorOrder(e.behavior())));
    }

    /**
     * Freezes this registry, preventing any further behavior registrations.
     */
    public void freeze() {
        this.frozen = true;
        logger.info("PipelineRegistry frozen with {} behaviors ({} global)",
                getBehaviorCount(), globalBehaviors.size());
    }

    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Resolves the execution order associated with the behavior instance.
     */
    private int getBehaviorOrder(PipelineBehavior<?, ?> behavior) {
        return calculateBehaviorOrder(behavior);
    }

    /**
     * Calculates the order based on {@link Order} or the {@link Ordered} contract.
     */
    private int calculateBehaviorOrder(PipelineBehavior<?, ?> behavior) {
        Class<?> behaviorClass = behavior.getClass();
        // Check for the @Order annotation
        Order orderAnnotation = behaviorClass.getAnnotation(Order.class);
        if (orderAnnotation != null) {
            return orderAnnotation.value();
        }

        // Check if it implements Ordered
        if (behavior instanceof Ordered orderedBehavior) {
            return orderedBehavior.getOrder();
        }

        // Default order (low priority)
        return Ordered.LOWEST_PRECEDENCE;
    }

    @SuppressWarnings("unchecked")
    public <R extends IRequest<S>, S> List<PipelineBehavior<R, S>> getBehaviors(Class<R> requestType) {
        List<PipelineBehavior<R, S>> result = new ArrayList<>();

        // Global behaviors — use stored request type resolved at registration
        // time (immune to Spring proxy classes that erase generic metadata).
        for (GlobalBehaviorEntry entry : globalBehaviors) {
            if (entry.requestType() != null
                    && entry.requestType().isAssignableFrom(requestType)) {
                result.add((PipelineBehavior<R, S>) entry.behavior());
            }
        }

        // Request-specific behaviors — the key is the already-resolved type,
        // so no need to re-extract from the (possibly proxied) runtime class.
        List<PipelineBehavior<?, ?>> behaviors = behaviorRegistry.get(requestType);
        if (behaviors != null) {
            for (PipelineBehavior<?, ?> behavior : behaviors) {
                result.add((PipelineBehavior<R, S>) behavior);
            }
        }

        result.sort(Comparator.comparingInt(this::getBehaviorOrder));
        return result;
    }

    public boolean hasBehaviorsFor(Class<?> requestType) {
        for (GlobalBehaviorEntry entry : globalBehaviors) {
            if (entry.requestType() != null
                    && entry.requestType().isAssignableFrom(requestType)) {
                return true;
            }
        }
        List<PipelineBehavior<?, ?>> behaviors = behaviorRegistry.get(requestType);
        return behaviors != null && !behaviors.isEmpty();
    }

    public int getBehaviorCount() {
        return globalBehaviors.size()
                + behaviorRegistry.values().stream().mapToInt(List::size).sum();
    }

    private Class<?> extractRequestTypeFromBehavior(Class<?> behaviorClass) {
        return BehaviorTypeResolver.extractRequestType(behaviorClass);
    }
}
