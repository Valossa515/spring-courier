package io.github.valossa515.pizzatracker.notification;

import io.github.valossa515.pizzatracker.domain.OrderStatus;
import io.github.valossa515.spring_courier.core.interfaces.INotification;

public class OrderStatusChangedNotification implements INotification {
    private final String orderId;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;
    private final String customerName;

    public OrderStatusChangedNotification(String orderId, OrderStatus oldStatus,
                                          OrderStatus newStatus, String customerName) {
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.customerName = customerName;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getOldStatus() {
        return oldStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public String getCustomerName() {
        return customerName;
    }
}
