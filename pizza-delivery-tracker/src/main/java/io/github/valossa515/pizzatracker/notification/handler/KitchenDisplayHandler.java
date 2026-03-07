package io.github.valossa515.pizzatracker.notification.handler;

import io.github.valossa515.pizzatracker.notification.OrderPlacedNotification;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KitchenDisplayHandler
        implements NotificationHandler<OrderPlacedNotification> {

    private static final Logger logger = LoggerFactory.getLogger(KitchenDisplayHandler.class);

    @Override
    public void handle(OrderPlacedNotification notification) {
        logger.info("[KITCHEN DISPLAY] New order received! "
                        + "Order #{} for {} - Total: R${}",
                notification.getOrderId(),
                notification.getCustomerName(),
                String.format("%.2f", notification.getTotalPrice()));
    }
}
