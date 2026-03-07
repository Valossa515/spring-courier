package io.github.valossa515.pizzatracker.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private final String id;
    private final String customerName;
    private final String customerPhone;
    private final String deliveryAddress;
    private final List<PizzaItem> items;
    private final LocalDateTime createdAt;
    private final List<StatusChange> statusHistory;
    private OrderStatus status;
    private String driverId;
    private LocalDateTime estimatedDelivery;

    public Order(String id, String customerName, String customerPhone,
                 String deliveryAddress, List<PizzaItem> items) {
        this.id = id;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.RECEIVED;
        this.createdAt = LocalDateTime.now();
        this.statusHistory = new ArrayList<>();
        this.statusHistory.add(new StatusChange(OrderStatus.RECEIVED, LocalDateTime.now()));
    }

    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public List<PizzaItem> getItems() {
        return List.copyOf(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.statusHistory.add(new StatusChange(status, LocalDateTime.now()));
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    public List<StatusChange> getStatusHistory() {
        return List.copyOf(statusHistory);
    }

    public double getTotalPrice() {
        return items.stream().mapToDouble(PizzaItem::getTotalPrice).sum();
    }

    public record StatusChange(OrderStatus status, LocalDateTime changedAt) {
    }
}
