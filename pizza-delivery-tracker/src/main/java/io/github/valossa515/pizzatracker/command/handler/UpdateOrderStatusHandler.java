package io.github.valossa515.pizzatracker.command.handler;

import io.github.valossa515.pizzatracker.command.UpdateOrderStatusCommand;
import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.pizzatracker.domain.OrderStatus;
import io.github.valossa515.pizzatracker.notification.OrderStatusChangedNotification;
import io.github.valossa515.pizzatracker.repository.OrderRepository;
import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.support.Response;
import org.springframework.stereotype.Component;

@Component
public class UpdateOrderStatusHandler
        implements CommandHandler<UpdateOrderStatusCommand, Response<String>> {

    private final OrderRepository orderRepository;
    private final Courier courier;

    public UpdateOrderStatusHandler(OrderRepository orderRepository, Courier courier) {
        this.orderRepository = orderRepository;
        this.courier = courier;
    }

    @Override
    public Response<String> handle(UpdateOrderStatusCommand command) {
        Order order = orderRepository.findById(command.getOrderId())
                .orElse(null);

        if (order == null) {
            return Response.error("Order not found: " + command.getOrderId(), 404);
        }

        OrderStatus oldStatus = order.getStatus();

        if (oldStatus == OrderStatus.CANCELLED) {
            return Response.error("Cannot update a cancelled order", 400);
        }
        if (oldStatus == OrderStatus.DELIVERED) {
            return Response.error("Cannot update a delivered order", 400);
        }

        order.setStatus(command.getNewStatus());
        orderRepository.save(order);

        courier.publish(new OrderStatusChangedNotification(
                order.getId(),
                oldStatus,
                command.getNewStatus(),
                order.getCustomerName()
        ));

        return Response.success("Order " + order.getId()
                + " updated to " + command.getNewStatus());
    }
}
