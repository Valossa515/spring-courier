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
        // Reordenar behaviors quando adicionar novo
        sortBehaviors(requestType);
    }

    /**
     * Obtém a ordem de execução de um behavior
     */
    private int getBehaviorOrder(PipelineBehavior<?, ?> behavior) {
        return calculateBehaviorOrder(behavior);
    }

    /**
     * Calcula a ordem baseado em @Order ou interface Ordered
     */
    private int calculateBehaviorOrder(PipelineBehavior<?, ?> behavior) {
        Class<?> behaviorClass = behavior.getClass();
        // Verifica annotation @Order
        Order orderAnnotation = behaviorClass.getAnnotation(Order.class);
        if (orderAnnotation != null) {
            return orderAnnotation.value();
        }

        // Verifica se implementa Ordered
        if (behavior instanceof Ordered orderedBehavior) {
            return orderedBehavior.getOrder();
        }

        // Ordem padrão (baixa prioridade)
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

        // Filtra apenas os behaviors que são compatíveis com o tipo de request
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

        // Extrai o tipo de request que o behavior espera
        Class<?> behaviorRequestType = extractRequestTypeFromBehavior(behavior.getClass());

        // Verifica compatibilidade
        return behaviorRequestType != null && behaviorRequestType.isAssignableFrom(requestType);
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

    public boolean hasBehaviorsFor(Class<?> requestType) {
        List<PipelineBehavior<?, ?>> behaviors = behaviorRegistry.get(requestType);
        return behaviors != null && !behaviors.isEmpty();
    }

    public int getBehaviorCount() {
        return behaviorRegistry.values().stream().mapToInt(List::size).sum();
    }
}