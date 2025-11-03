package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import org.slf4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PipelineRegistry {
    private final Map<Class<?>, List<PipelineBehavior<?, ?>>> behaviorRegistry = new ConcurrentHashMap<>();
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(PipelineRegistry.class);

    public void registerBehavior(Class<?> requestType, PipelineBehavior<?, ?> behavior) {
        behaviorRegistry.computeIfAbsent(requestType, k -> new ArrayList<>()).add(behavior);
        // Reorder behaviors whenever a new one is added
        sortBehaviors(requestType);
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

    private void sortBehaviors(Class<?> requestType) {
        List<PipelineBehavior<?, ?>> behaviors = behaviorRegistry.get(requestType);
        if (behaviors != null) {
            behaviors.sort(Comparator.comparingInt(this::getBehaviorOrder));
        }
    }

    @SuppressWarnings("unchecked")
    public <TRequest extends IRequest<TResponse>, TResponse> List<PipelineBehavior<TRequest, TResponse>> getBehaviors(Class<TRequest> requestType) {
        List<PipelineBehavior<?, ?>> behaviors = behaviorRegistry.get(requestType);
        if (behaviors == null) {
            return Collections.emptyList();
        }

        // Include only behaviors compatible with the request type
        List<PipelineBehavior<TRequest, TResponse>> compatibleBehaviors = new ArrayList<>();

        for (PipelineBehavior<?, ?> behavior : behaviors) {
            if (isBehaviorCompatible(behavior, requestType)) {
                compatibleBehaviors.add((PipelineBehavior<TRequest, TResponse>) behavior);
            }
        }

        return compatibleBehaviors;
    }

    private <TRequest extends IRequest<TResponse>, TResponse> boolean isBehaviorCompatible(
            PipelineBehavior<?, ?> behavior, Class<TRequest> requestType) {

        // Extract the request type expected by the behavior
        Class<?> behaviorRequestType = extractRequestTypeFromBehavior(behavior.getClass());

        // Check compatibility
        return behaviorRequestType != null && behaviorRequestType.isAssignableFrom(requestType);
    }

    private Class<?> extractRequestTypeFromBehavior(Class<?> behaviorClass) {
        // Inspect generic interfaces
        Type[] genericInterfaces = behaviorClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            Class<?> requestType = extractRequestTypeFromParameterizedType(genericInterface);
            if (requestType != null) {
                return requestType;
            }
        }

        // Inspect generic superclass
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

    public boolean hasBehaviorsFor(Class<?> requestType) {
        List<PipelineBehavior<?, ?>> behaviors = behaviorRegistry.get(requestType);
        return behaviors != null && !behaviors.isEmpty();
    }

    public int getBehaviorCount() {
        return behaviorRegistry.values().stream().mapToInt(List::size).sum();
    }
}