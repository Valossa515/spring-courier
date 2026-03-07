package io.github.valossa515.pizzatracker.notification;

import io.github.valossa515.spring_courier.core.interfaces.INotification;

public class OrderPlacedNotification implements INotification {
    private final String orderId;
    private final String customerName;
    private final double totalPrice;

    public OrderPlacedNotification(String orderId, String customerName, double totalPrice) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.totalPrice = totalPrice;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
}
