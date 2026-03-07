package io.github.valossa515.pizzatracker.notification;

import io.github.valossa515.spring_courier.core.interfaces.INotification;

public class DriverAssignedNotification implements INotification {
    private final String orderId;
    private final String driverId;
    private final String driverName;
    private final String deliveryAddress;

    public DriverAssignedNotification(String orderId, String driverId,
                                      String driverName, String deliveryAddress) {
        this.orderId = orderId;
        this.driverId = driverId;
        this.driverName = driverName;
        this.deliveryAddress = deliveryAddress;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }
}
