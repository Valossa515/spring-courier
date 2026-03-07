package io.github.valossa515.pizzatracker.notification.handler;

import io.github.valossa515.pizzatracker.notification.OrderPlacedNotification;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

@Component
public class AnalyticsHandler
        implements NotificationHandler<OrderPlacedNotification> {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsHandler.class);

    private final AtomicInteger totalOrders = new AtomicInteger(0);
    private final DoubleAdder totalRevenue = new DoubleAdder();

    @Override
    public void handle(OrderPlacedNotification notification) {
        int count = totalOrders.incrementAndGet();
        totalRevenue.add(notification.getTotalPrice());

        logger.info("[ANALYTICS] Order #{} recorded. "
                        + "Total orders today: {} | Revenue: R${}",
                notification.getOrderId(),
                count,
                String.format("%.2f", totalRevenue.sum()));
    }

    public int getTotalOrders() {
        return totalOrders.get();
    }

    public double getTotalRevenue() {
        return totalRevenue.sum();
    }
}
