package io.github.valossa515.pizzatracker.query;

import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.support.Response;

public class GetOrderQuery implements IQuery<Response<Order>> {
    private final String orderId;

    public GetOrderQuery(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
