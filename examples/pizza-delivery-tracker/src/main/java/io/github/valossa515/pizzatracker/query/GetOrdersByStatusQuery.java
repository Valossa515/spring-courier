package io.github.valossa515.pizzatracker.query;

import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.pizzatracker.domain.OrderStatus;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;
import io.github.valossa515.spring_courier.core.support.Response;

import java.util.List;

public class GetOrdersByStatusQuery implements IQuery<Response<List<Order>>> {
    private final OrderStatus status;

    public GetOrdersByStatusQuery(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
