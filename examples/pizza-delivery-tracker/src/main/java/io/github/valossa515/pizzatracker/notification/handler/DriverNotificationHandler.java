package io.github.valossa515.pizzatracker.notification.handler;

import io.github.valossa515.pizzatracker.notification.DriverAssignedNotification;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DriverNotificationHandler
        implements NotificationHandler<DriverAssignedNotification> {

    private static final Logger logger = LoggerFactory.getLogger(DriverNotificationHandler.class);

    @Override
    public void handle(DriverAssignedNotification notification) {
        logger.info("[DRIVER APP -> {}] New delivery! "
                        + "Order #{} to address: {}",
                notification.getDriverName(),
                notification.getOrderId(),
                notification.getDeliveryAddress());
    }
}
