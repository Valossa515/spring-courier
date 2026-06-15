package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PostProcessor;
import io.github.valossa515.spring_courier.core.pipelines.PreProcessor;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers GraalVM Native Image reflection hints for Spring Courier
 * core types that are accessed via reflection (handler method
 * invocation, generic type resolution, etc.).
 *
 * <p>Activated via {@code @ImportRuntimeHints} in
 * {@link CourierAutoConfiguration} or as a
 * {@code META-INF/spring/aot.factories} entry.
 */
public class CourierRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints,
            ClassLoader classLoader) {

        // Core dispatcher and response
        registerReflection(hints, Courier.class);
        registerReflection(hints, Response.class);

        // Handler interfaces — method.invoke() on user implementations
        registerReflection(hints, CommandHandler.class);
        registerReflection(hints, QueryHandler.class);
        registerReflection(hints, NotificationHandler.class);

        // Request hierarchy — generic type resolution at runtime
        registerReflection(hints, IRequest.class);
        registerReflection(hints, ICommand.class);
        registerReflection(hints, IQuery.class);
        registerReflection(hints, INotification.class);

        // Pipeline & processor interfaces
        registerReflection(hints, PipelineBehavior.class);
        registerReflection(hints, PreProcessor.class);
        registerReflection(hints, PostProcessor.class);

        // Registries
        registerReflection(hints, HandlerRegistry.class);
        registerReflection(hints, NotificationRegistry.class);
        registerReflection(hints, ProcessorRegistry.class);
    }

    private void registerReflection(RuntimeHints hints, Class<?> type) {
        hints.reflection().registerType(type,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.DECLARED_FIELDS);
    }
}
