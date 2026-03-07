package io.github.valossa515.pizzatracker.query.handler;

import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.pizzatracker.query.GetActiveOrdersQuery;
import io.github.valossa515.pizzatracker.repository.OrderRepository;
import io.github.valossa515.spring_courier.core.interfaces.QueryHandler;
import io.github.valossa515.spring_courier.core.support.Response;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetActiveOrdersHandler
        implements QueryHandler<GetActiveOrdersQuery, Response<List<Order>>> {

    private final OrderRepository orderRepository;

    public GetActiveOrdersHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Response<List<Order>> handle(GetActiveOrdersQuery query) {
        List<Order> activeOrders = orderRepository.findActiveOrders();
        return Response.success(activeOrders);
    }
}
