package io.github.valossa515.spring_courier.core;

import io.github.valossa515.spring_courier.core.pipelines.PipelineExecutor;
import io.github.valossa515.spring_courier.core.pipelines.PipelineRegistry;
import io.github.valossa515.spring_courier.core.support.HandlerRegistry;
import io.github.valossa515.spring_courier.core.support.NotificationRegistry;

/**
 * Reusable test fixture that wires the minimum set of real components required
 * to exercise {@link Courier} in unit / integration tests.
 *
 * <p>Usage:
 * <pre>{@code
 *   CourierTestFixture fixture = CourierTestFixture.create();
 *   fixture.handlerRegistry().registerHandler(MyReq.class, new MyHandler());
 *   Response<String> resp = fixture.courier().send(new MyReq());
 * }</pre>
 */
public final class CourierTestFixture {

    private final HandlerRegistry handlerRegistry;
    private final NotificationRegistry notificationRegistry;
    private final PipelineRegistry pipelineRegistry;
    private final PipelineExecutor pipelineExecutor;
    private final Courier courier;

    private CourierTestFixture(HandlerRegistry handlerRegistry,
                               NotificationRegistry notificationRegistry,
                               PipelineRegistry pipelineRegistry,
                               PipelineExecutor pipelineExecutor,
                               Courier courier) {
        this.handlerRegistry = handlerRegistry;
        this.notificationRegistry = notificationRegistry;
        this.pipelineRegistry = pipelineRegistry;
        this.pipelineExecutor = pipelineExecutor;
        this.courier = courier;
    }

    /**
     * Creates a fixture with default real components and the default async timeout.
     */
    public static CourierTestFixture create() {
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        NotificationRegistry notificationRegistry = new NotificationRegistry();
        PipelineRegistry pipelineRegistry = new PipelineRegistry();
        PipelineExecutor pipelineExecutor = new PipelineExecutor(pipelineRegistry);
        Courier courier = new Courier(handlerRegistry, notificationRegistry, pipelineExecutor);
        return new CourierTestFixture(handlerRegistry, notificationRegistry,
                pipelineRegistry, pipelineExecutor, courier);
    }

    public HandlerRegistry handlerRegistry() {
        return handlerRegistry;
    }

    public NotificationRegistry notificationRegistry() {
        return notificationRegistry;
    }

    public PipelineRegistry pipelineRegistry() {
        return pipelineRegistry;
    }

    public PipelineExecutor pipelineExecutor() {
        return pipelineExecutor;
    }

    public Courier courier() {
        return courier;
    }
}
