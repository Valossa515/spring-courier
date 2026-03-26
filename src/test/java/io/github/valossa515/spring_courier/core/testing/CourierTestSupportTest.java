package io.github.valossa515.spring_courier.core.testing;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import io.github.valossa515.spring_courier.core.pipelines.PipelineBehavior;
import io.github.valossa515.spring_courier.core.support.NotificationPublishingStrategy;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierTestSupportTest {

    record EchoCmd(String msg) implements ICommand<String> {
    }

    record EchoQuery(String msg) implements IQuery<String> {
    }

    record TestEvent(String data) implements INotification {
    }

    static class EchoCmdHandler
            implements CommandHandler<EchoCmd, String> {
        @Override
        public String handle(EchoCmd command) {
            return "cmd:" + command.msg();
        }
    }

    static class EchoQueryHandler
            implements QueryHandler<EchoQuery, String> {
        @Override
        public String handle(EchoQuery query) {
            return "qry:" + query.msg();
        }
    }

    @Test
    void builderCreatesWorkingCourier() {
        Courier courier = CourierTestSupport.builder()
                .withHandler(EchoCmd.class,
                        new EchoCmdHandler())
                .build();

        Response<String> result =
                courier.send(new EchoCmd("hello"));

        assertTrue(result.isSuccess());
        assertEquals("cmd:hello", result.getData());
    }

    @Test
    void builderSupportsMultipleHandlers() {
        Courier courier = CourierTestSupport.builder()
                .withHandler(EchoCmd.class,
                        new EchoCmdHandler())
                .withHandler(EchoQuery.class,
                        new EchoQueryHandler())
                .build();

        assertEquals("cmd:a",
                courier.send(new EchoCmd("a")).getData());
        assertEquals("qry:b",
                courier.send(new EchoQuery("b")).getData());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void builderSupportsBehaviors() {
        AtomicBoolean behaviorCalled = new AtomicBoolean(false);

        PipelineBehavior behavior =
                (PipelineBehavior) (request, next) -> {
                    behaviorCalled.set(true);
                    return ((PipelineBehavior.Next) next).invoke();
                };

        var courier = CourierTestSupport.builder()
                .withHandler(EchoCmd.class,
                        new EchoCmdHandler())
                .withBehavior(behavior)
                .build();

        courier.send(new EchoCmd("test"));
        assertTrue(behaviorCalled.get());
    }

    @Test
    void builderSupportsPreProcessor() {
        AtomicReference<String> captured =
                new AtomicReference<>();

        Courier courier = CourierTestSupport.builder()
                .withHandler(EchoCmd.class,
                        new EchoCmdHandler())
                .withPreProcessor(req ->
                        captured.set(
                                req.getClass().getSimpleName()))
                .build();

        courier.send(new EchoCmd("test"));
        assertEquals("EchoCmd", captured.get());
    }

    @Test
    void builderSupportsNotificationHandlers() {
        AtomicReference<String> captured =
                new AtomicReference<>();

        Courier courier = CourierTestSupport.builder()
                .withNotificationHandler(TestEvent.class,
                        event -> captured.set(event.data()))
                .build();

        courier.publish(new TestEvent("hello"));
        assertEquals("hello", captured.get());
    }

    @Test
    void builderSupportsCustomTimeout() {
        Courier courier = CourierTestSupport.builder()
                .withHandler(EchoCmd.class,
                        new EchoCmdHandler())
                .withTimeout(5000)
                .build();

        assertNotNull(courier);
        Response<String> result =
                courier.send(new EchoCmd("fast"));
        assertTrue(result.isSuccess());
    }

    @Test
    void builderSupportsNotificationStrategy() {
        Courier courier = CourierTestSupport.builder()
                .withNotificationStrategy(
                        NotificationPublishingStrategy
                                .PARALLEL_WHEN_ALL)
                .build();

        assertNotNull(courier);
    }

    @Test
    void emptyCourierCreatesMinimalInstance() {
        Courier courier = CourierTestSupport.emptyCourier();
        assertNotNull(courier);
    }
}
