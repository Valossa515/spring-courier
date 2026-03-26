package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.annotations.Timeout;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.pipelines.ProcessorRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierTimeoutAnnotationTest {

    private Courier courier;

    @Timeout(200)
    record ShortTimeoutCmd() implements ICommand<String> {
    }

    record NormalCmd() implements ICommand<String> {
    }

    static class SlowHandler
            implements CommandHandler<ShortTimeoutCmd, String> {
        @Override
        public String handle(ShortTimeoutCmd command) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "done";
        }
    }

    static class FastHandler
            implements CommandHandler<ShortTimeoutCmd, String> {
        @Override
        public String handle(ShortTimeoutCmd command) {
            return "fast";
        }
    }

    static class NormalHandler
            implements CommandHandler<NormalCmd, String> {
        @Override
        public String handle(NormalCmd command) {
            return "normal";
        }
    }

    @AfterEach
    void cleanup() {
        Courier.clearMethodCaches();
    }

    private Courier buildCourier(HandlerRegistry registry) {
        NotificationRegistry notificationRegistry =
                new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        PipelineExecutor pipelineExecutor =
                new PipelineExecutor(pipelineRegistry);
        return new Courier(registry, notificationRegistry,
                pipelineExecutor, new ProcessorRegistry(),
                null, 30_000,
                NotificationPublishingStrategy.SEQUENTIAL);
    }

    @Test
    void timeoutAnnotationOverridesGlobalTimeout() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.registerHandler(
                ShortTimeoutCmd.class, new SlowHandler());
        courier = buildCourier(registry);

        Response<String> response =
                courier.send(new ShortTimeoutCmd());

        assertFalse(response.isSuccess());
        assertEquals(504, response.getStatusCode());
        assertTrue(response.getError().contains("200ms"));
    }

    @Test
    void fastHandlerWithTimeoutAnnotationSucceeds() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.registerHandler(
                ShortTimeoutCmd.class, new FastHandler());
        courier = buildCourier(registry);

        Response<String> response =
                courier.send(new ShortTimeoutCmd());

        assertTrue(response.isSuccess());
        assertEquals("fast", response.getData());
    }

    @Test
    void requestWithoutAnnotationUsesGlobalTimeout() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.registerHandler(
                NormalCmd.class, new NormalHandler());
        courier = buildCourier(registry);

        Response<String> response =
                courier.send(new NormalCmd());

        assertTrue(response.isSuccess());
        assertEquals("normal", response.getData());
    }

    @Test
    void timeoutAnnotationHasCorrectValue() {
        Timeout annotation =
                ShortTimeoutCmd.class.getAnnotation(Timeout.class);
        assertEquals(200, annotation.value());
    }
}
