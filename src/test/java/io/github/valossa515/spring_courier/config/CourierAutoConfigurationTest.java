package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.CourierMetrics;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.BehaviorDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourierAutoConfigurationTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<CourierMetrics> emptyMetricsProvider() {
        return mock(ObjectProvider.class);
    }

    @Test
    void buildsCoreBeans() {
        CourierAutoConfiguration configuration = new CourierAutoConfiguration();
        CourierProperties properties = new CourierProperties();

        HandlerRegistry handlerRegistry = configuration.handlerRegistry();
        NotificationRegistry notificationRegistry = configuration.notificationRegistry();
        PipelineRegistry pipelineRegistry = configuration.pipelineRegistry();
        PipelineExecutor pipelineExecutor = configuration.pipelineExecutor(pipelineRegistry);
        @SuppressWarnings("unchecked")
        ObjectProvider<Executor> executorProvider = mock(ObjectProvider.class);
        Courier courier = configuration.courier(
                handlerRegistry, notificationRegistry, pipelineExecutor,
                properties, executorProvider, emptyMetricsProvider());

        assertNotNull(handlerRegistry);
        assertNotNull(notificationRegistry);
        assertNotNull(pipelineRegistry);
        assertNotNull(pipelineExecutor);
        assertNotNull(courier);
    }

    @Test
    void buildsPostProcessors() {
        CourierAutoConfiguration configuration = new CourierAutoConfiguration();
        ApplicationContext context = mock(ApplicationContext.class);

        HandlerRegistry handlerRegistry = configuration.handlerRegistry();
        NotificationRegistry notificationRegistry = configuration.notificationRegistry();
        PipelineRegistry pipelineRegistry = configuration.pipelineRegistry();

        HandlerDiscoveryPostProcessor handlerPostProcessor =
                configuration.handlerDiscoveryPostProcessor(handlerRegistry, context);
        NotificationDiscoveryPostProcessor notificationPostProcessor =
                configuration.notificationDiscoveryPostProcessor(notificationRegistry, context);
        BehaviorDiscoveryPostProcessor behaviorPostProcessor =
                configuration.behaviorDiscoveryPostProcessor(pipelineRegistry);

        assertNotNull(handlerPostProcessor);
        assertNotNull(notificationPostProcessor);
        assertNotNull(behaviorPostProcessor);
    }

    @Test
    void courierUsesGetIfUniqueForExecutorLookup() {
        CourierAutoConfiguration configuration = new CourierAutoConfiguration();
        CourierProperties properties = new CourierProperties();
        HandlerRegistry handlerRegistry = configuration.handlerRegistry();
        NotificationRegistry notificationRegistry = configuration.notificationRegistry();
        PipelineRegistry pipelineRegistry = configuration.pipelineRegistry();
        PipelineExecutor pipelineExecutor = configuration.pipelineExecutor(pipelineRegistry);

        @SuppressWarnings("unchecked")
        ObjectProvider<Executor> executorProvider = mock(ObjectProvider.class);

        configuration.courier(
                handlerRegistry, notificationRegistry, pipelineExecutor,
                properties, executorProvider, emptyMetricsProvider());

        verify(executorProvider).getIfUnique();
    }

    @Test
    void courierReceivesExecutorWhenUniqueBeanExists() {
        CourierAutoConfiguration configuration = new CourierAutoConfiguration();
        CourierProperties properties = new CourierProperties();
        HandlerRegistry handlerRegistry = configuration.handlerRegistry();
        NotificationRegistry notificationRegistry = configuration.notificationRegistry();
        PipelineRegistry pipelineRegistry = configuration.pipelineRegistry();
        PipelineExecutor pipelineExecutor = configuration.pipelineExecutor(pipelineRegistry);

        Executor expectedExecutor = Runnable::run;
        @SuppressWarnings("unchecked")
        ObjectProvider<Executor> executorProvider = mock(ObjectProvider.class);
        when(executorProvider.getIfUnique()).thenReturn(expectedExecutor);

        Courier courier = configuration.courier(
                handlerRegistry, notificationRegistry, pipelineExecutor,
                properties, executorProvider, emptyMetricsProvider());

        assertNotNull(courier);
        verify(executorProvider).getIfUnique();
    }

    @Test
    void courierReceivesNullWhenMultipleExecutorsExist() {
        CourierAutoConfiguration configuration = new CourierAutoConfiguration();
        CourierProperties properties = new CourierProperties();
        HandlerRegistry handlerRegistry = configuration.handlerRegistry();
        NotificationRegistry notificationRegistry = configuration.notificationRegistry();
        PipelineRegistry pipelineRegistry = configuration.pipelineRegistry();
        PipelineExecutor pipelineExecutor = configuration.pipelineExecutor(pipelineRegistry);

        @SuppressWarnings("unchecked")
        ObjectProvider<Executor> executorProvider = mock(ObjectProvider.class);
        when(executorProvider.getIfUnique()).thenReturn(null);

        Courier courier = configuration.courier(
                handlerRegistry, notificationRegistry, pipelineExecutor,
                properties, executorProvider, emptyMetricsProvider());

        assertNotNull(courier);
        verify(executorProvider).getIfUnique();
    }
}
