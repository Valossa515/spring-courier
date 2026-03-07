package io.github.valossa515.pizzatracker.command.handler;

import io.github.valossa515.pizzatracker.command.CancelOrderCommand;
import io.github.valossa515.pizzatracker.domain.Driver;
import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.pizzatracker.domain.OrderStatus;
import io.github.valossa515.pizzatracker.notification.OrderStatusChangedNotification;
import io.github.valossa515.pizzatracker.repository.DriverRepository;
import io.github.valossa515.pizzatracker.repository.OrderRepository;
import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.support.Response;
import org.springframework.stereotype.Component;

@Component
public class CancelOrderHandler
        implements CommandHandler<CancelOrderCommand, Response<String>> {

    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final Courier courier;

    public CancelOrderHandler(OrderRepository orderRepository,
                              DriverRepository driverRepository,
                              Courier courier) {
        this.orderRepository = orderRepository;
        this.driverRepository = driverRepository;
        this.courier = courier;
    }

    @Override
    public Response<String> handle(CancelOrderCommand command) {
        Order order = orderRepository.findById(command.getOrderId()).orElse(null);
        if (order == null) {
            return Response.error("Order not found: " + command.getOrderId(), 404);
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            return Response.error("Cannot cancel a delivered order", 400);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return Response.error("Order is already cancelled", 400);
        }

        OrderStatus oldStatus = order.getStatus();

        if (order.getDriverId() != null) {
            driverRepository.findById(order.getDriverId())
                    .ifPresent(driver -> {
                        driver.setAvailable(true);
                        driverRepository.save(driver);
                    });
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        courier.publish(new OrderStatusChangedNotification(
                order.getId(), oldStatus, OrderStatus.CANCELLED,
                order.getCustomerName()
        ));

        return Response.success("Order " + order.getId()
                + " cancelled. Reason: " + command.getReason());
    }
}
