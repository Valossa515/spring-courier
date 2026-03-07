package io.github.valossa515.pizzatracker.notification.handler;

import io.github.valossa515.pizzatracker.notification.OrderStatusChangedNotification;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerSmsHandler
        implements NotificationHandler<OrderStatusChangedNotification> {

    private static final Logger logger = LoggerFactory.getLogger(CustomerSmsHandler.class);

    @Override
    public void handle(OrderStatusChangedNotification notification) {
        String message = switch (notification.getNewStatus()) {
            case PREPARING -> "Your pizza is being prepared!";
            case IN_OVEN -> "Your pizza is in the oven!";
            case READY -> "Your pizza is ready and waiting for a driver!";
            case OUT_FOR_DELIVERY -> "Your pizza is on its way!";
            case DELIVERED -> "Your pizza has been delivered. Enjoy!";
            case CANCELLED -> "Your order has been cancelled.";
            default -> "Order status updated to " + notification.getNewStatus();
        };

        logger.info("[SMS -> {}] Order #{}: {}",
                notification.getCustomerName(),
                notification.getOrderId(),
                message);
    }
}
