package io.github.valossa515.pizzatracker.command;

import io.github.valossa515.pizzatracker.domain.OrderStatus;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.support.Response;

public class UpdateOrderStatusCommand implements ICommand<Response<String>> {
    private final String orderId;
    private final OrderStatus newStatus;

    public UpdateOrderStatusCommand(String orderId, OrderStatus newStatus) {
        this.orderId = orderId;
        this.newStatus = newStatus;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }
}
