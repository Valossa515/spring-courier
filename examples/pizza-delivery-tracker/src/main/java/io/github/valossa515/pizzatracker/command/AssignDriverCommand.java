package io.github.valossa515.pizzatracker.command;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.support.Response;

public class AssignDriverCommand implements ICommand<Response<String>> {
    private final String orderId;
    private final String driverId;

    public AssignDriverCommand(String orderId, String driverId) {
        this.orderId = orderId;
        this.driverId = driverId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDriverId() {
        return driverId;
    }
}
