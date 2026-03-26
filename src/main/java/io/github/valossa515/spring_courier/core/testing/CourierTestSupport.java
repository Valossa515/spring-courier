package io.github.valossa515.spring_courier.core.testing;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.PostProcessor;
import io.github.valossa515.spring_courier.core.pipelines.PreProcessor;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;

import java.util.concurrent.Executor;

/**
 * Fluent builder for constructing a fully-wired {@link Courier}
 * instance in unit and integration tests, without requiring a
 * Spring application context.
 *
 * <p>Eliminates the boilerplate of manually creating registries
 * and executors:
 *
 * <pre>{@code
 * Courier courier = CourierTestSupport.builder()
 *     .withHandler(CreateOrder.class, new CreateOrderHandler())
 *     .withHandler(GetOrder.class, new GetOrderHandler())
 *     .withBehavior(new LoggingBehavior<>())
 *     .withPreProcessor(req -> log.info("pre: {}", req))
 *     .withTimeout(5000)
 *     .build();
 *
 * Response<OrderResult> result = courier.send(new CreateOrder(...));
 * }</pre>
 */
public final class CourierTestSupport {

    private CourierTestSupport() {
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Shortcut: creates a minimal Courier with no handlers or
     * behaviors registered. Equivalent to
     * {@code CourierTestSupport.builder().build()}.
     */
    public static Courier emptyCourier() {
        return builder().build();
    }

    public static final class Builder {
        private final HandlerRegistry handlerRegistry =
                new HandlerRegistry();
        private final NotificationRegistry notificationRegistry =
                new NotificationRegistry();
        private final PipelineRegistry pipelineRegistry =
                new PipelineRegistry();
        private final ProcessorRegistry processorRegistry =
                new ProcessorRegistry();
        private Executor asyncExecutor;
        private long asyncTimeoutMs = 30_000;
        private NotificationPublishingStrategy notificationStrategy =
                NotificationPublishingStrategy.SEQUENTIAL;

        private Builder() {
        }

        /**
         * Registers a request handler.
         *
         * @param requestType the request class
         * @param handler     the handler instance
         */
        public <R> Builder withHandler(
                Class<? extends IRequest<R>> requestType,
                Object handler) {
            handlerRegistry.registerHandler(requestType, handler);
            return this;
        }

        /**
         * Registers a notification handler.
         *
         * @param notificationType the notification class
         * @param handler          the handler instance
         */
        public <N extends INotification> Builder withNotificationHandler(
                Class<N> notificationType,
                NotificationHandler<N> handler) {
            notificationRegistry.registerHandler(
                    notificationType, handler);
            return this;
        }

        /**
         * Registers a global pipeline behavior.
         */
        public Builder withBehavior(
                PipelineBehavior<?, ?> behavior) {
            pipelineRegistry.registerGlobalBehavior(behavior,
                    IRequest.class);
            return this;
        }

        /**
         * Registers a pre-processor.
         */
        public Builder withPreProcessor(
                PreProcessor<?> preProcessor) {
            processorRegistry.registerPreProcessor(preProcessor);
            return this;
        }

        /**
         * Registers a post-processor.
         */
        public Builder withPostProcessor(
                PostProcessor<?, ?> postProcessor) {
            processorRegistry.registerPostProcessor(postProcessor);
            return this;
        }

        /**
         * Sets the async timeout in milliseconds.
         */
        public Builder withTimeout(long timeoutMs) {
            this.asyncTimeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets a custom async executor.
         */
        public Builder withExecutor(Executor executor) {
            this.asyncExecutor = executor;
            return this;
        }

        /**
         * Sets the notification publishing strategy.
         */
        public Builder withNotificationStrategy(
                NotificationPublishingStrategy strategy) {
            this.notificationStrategy = strategy;
            return this;
        }

        /**
         * Builds the Courier instance with all registered
         * components.
         */
        public Courier build() {
            PipelineExecutor pipelineExecutor =
                    new PipelineExecutor(pipelineRegistry);
            return new Courier(
                    handlerRegistry,
                    notificationRegistry,
                    pipelineExecutor,
                    processorRegistry,
                    asyncExecutor,
                    asyncTimeoutMs,
                    notificationStrategy);
        }

        /**
         * Returns the handler registry for additional
         * configuration.
         */
        public HandlerRegistry handlerRegistry() {
            return handlerRegistry;
        }

        /**
         * Returns the notification registry for additional
         * configuration.
         */
        public NotificationRegistry notificationRegistry() {
            return notificationRegistry;
        }

        /**
         * Returns the pipeline registry for additional
         * configuration.
         */
        public PipelineRegistry pipelineRegistry() {
            return pipelineRegistry;
        }

        /**
         * Returns the processor registry for additional
         * configuration.
         */
        public ProcessorRegistry processorRegistry() {
            return processorRegistry;
        }
    }
}
