package io.github.valossa515.spring_courier.core.pipelines;

/**
 * Helper for behaviors that derive cache/idempotency keys from a
 * request's {@code toString()} representation.
 *
 * <p>Classes that do not override {@link Object#toString()} produce a
 * different key for every instance ({@code ClassName@hashCode}), which
 * would silently disable deduplication while filling the store with
 * never-matching entries. Behaviors use {@link #hasCustomToString(Class)}
 * to detect and skip such request types.
 */
final class RequestKeySupport {

    private RequestKeySupport() {
    }

    /**
     * Returns {@code true} if the given type overrides
     * {@link Object#toString()} (directly or via a superclass/record),
     * making it usable as a stable key source.
     */
    static boolean hasCustomToString(Class<?> type) {
        try {
            return type.getMethod("toString").getDeclaringClass()
                    != Object.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
