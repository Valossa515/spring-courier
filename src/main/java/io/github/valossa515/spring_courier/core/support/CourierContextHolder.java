package io.github.valossa515.spring_courier.core.support;

/**
 * Thread-local holder for the current {@link CourierContext}.
 *
 * <p>The context is set automatically by the Courier dispatcher before
 * sending a request and cleared after the request completes.
 * Pipeline behaviors, pre/post processors, and handlers can read the
 * current context at any time via {@link #getContext()}.
 *
 * <p>For async operations the context is propagated to the child thread.
 */
public final class CourierContextHolder {

    private static final ThreadLocal<CourierContext> HOLDER =
            new ThreadLocal<>();

    private CourierContextHolder() {
    }

    /**
     * Returns the current context, or creates a new one if none is set.
     */
    public static CourierContext getContext() {
        CourierContext ctx = HOLDER.get();
        if (ctx == null) {
            ctx = CourierContext.create();
            HOLDER.set(ctx);
        }
        return ctx;
    }

    /**
     * Returns the current context without creating one if absent.
     *
     * @return the current context, or {@code null} if none is set
     */
    public static CourierContext peekContext() {
        return HOLDER.get();
    }

    /**
     * Sets the current context.
     */
    public static void setContext(CourierContext context) {
        HOLDER.set(context);
    }

    /**
     * Clears the current context.
     */
    public static void clear() {
        HOLDER.remove();
    }
}
