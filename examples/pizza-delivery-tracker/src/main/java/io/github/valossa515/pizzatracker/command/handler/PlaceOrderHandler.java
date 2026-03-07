package io.github.valossa515.pizzatracker.command.handler;

import io.github.valossa515.pizzatracker.command.PlaceOrderCommand;
import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.pizzatracker.domain.PizzaItem;
import io.github.valossa515.pizzatracker.notification.OrderPlacedNotification;
import io.github.valossa515.pizzatracker.repository.OrderRepository;
import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.support.Response;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PlaceOrderHandler
        implements CommandHandler<PlaceOrderCommand, Response<String>> {

    private final OrderRepository orderRepository;
    private final Courier courier;

    public PlaceOrderHandler(OrderRepository orderRepository, Courier courier) {
        this.orderRepository = orderRepository;
        this.courier = courier;
    }

    @Override
    public Response<String> handle(PlaceOrderCommand command) {
        String orderId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        var pizzaItems = command.getItems().stream()
                .map(dto -> new PizzaItem(dto.flavor(), dto.size(), dto.quantity()))
                .toList();

        Order order = new Order(
                orderId,
                command.getCustomerName(),
                command.getCustomerPhone(),
                command.getDeliveryAddress(),
                pizzaItems
        );

        orderRepository.save(order);

        courier.publish(new OrderPlacedNotification(
                orderId,
                command.getCustomerName(),
                order.getTotalPrice()
        ));

        return Response.success(orderId, 201);
    }
}
