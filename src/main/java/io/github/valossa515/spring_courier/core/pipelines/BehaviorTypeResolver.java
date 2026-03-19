package io.github.valossa515.spring_courier.core.pipelines;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Resolves the request type ({@code R}) from a {@link PipelineBehavior}
 * implementation class by inspecting its generic type arguments.
 *
 * <p>Handles both concrete type arguments ({@code MyCommand}) and
 * parameterised ones ({@code IRequest<Response<?>>}), returning the
 * raw {@link Class} in the latter case.
 */
public final class BehaviorTypeResolver {

    private BehaviorTypeResolver() {
    }

    /**
     * Extracts the request type from a class that implements
     * {@link PipelineBehavior}. Returns {@code null} when the type
     * cannot be resolved (e.g. fully-erased type variables).
     */
    public static Class<?> extractRequestType(Class<?> behaviorClass) {
        Type[] genericInterfaces = behaviorClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            Class<?> requestType = extractFromParameterizedType(genericInterface);
            if (requestType != null) {
                return requestType;
            }
        }
        Type genericSuperclass = behaviorClass.getGenericSuperclass();
        return extractFromParameterizedType(genericSuperclass);
    }

    private static Class<?> extractFromParameterizedType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> rawClass
                    && PipelineBehavior.class.isAssignableFrom(rawClass)) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0) {
                    return resolveClass(typeArguments[0]);
                }
            }
        }
        return null;
    }

    private static Class<?> resolveClass(Type typeArg) {
        if (typeArg instanceof Class<?> clazz) {
            return clazz;
        }
        if (typeArg instanceof ParameterizedType pt
                && pt.getRawType() instanceof Class<?> rawClass) {
            return rawClass;
        }
        return null;
    }
}
