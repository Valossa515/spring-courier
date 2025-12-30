package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.BehaviorDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationDiscoveryPostProcessor;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class CourierAutoConfigurationTest {

    @Test
    void buildsCoreBeans() {
        CourierAutoConfiguration configuration = new CourierAutoConfiguration();

        HandlerRegistry handlerRegistry = configuration.handlerRegistry();
        NotificationRegistry notificationRegistry = configuration.notificationRegistry();
        PipelineRegistry pipelineRegistry = configuration.pipelineRegistry();
        PipelineExecutor pipelineExecutor = configuration.pipelineExecutor(pipelineRegistry);
        Courier courier = configuration.courier(handlerRegistry, notificationRegistry, pipelineExecutor);

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
}
