package io.github.valossa515.pizzatracker.query.handler;

import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.pizzatracker.query.GetOrderQuery;
import io.github.valossa515.pizzatracker.repository.OrderRepository;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import io.github.valossa515.spring_courier.core.support.Response;
import org.springframework.stereotype.Component;

@Component
public class GetOrderHandler
        implements QueryHandler<GetOrderQuery, Response<Order>> {

    private final OrderRepository orderRepository;

    public GetOrderHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Response<Order> handle(GetOrderQuery query) {
        return orderRepository.findById(query.getOrderId())
                .map(Response::success)
                .orElse(Response.error("Order not found: " + query.getOrderId(), 404));
    }
}
