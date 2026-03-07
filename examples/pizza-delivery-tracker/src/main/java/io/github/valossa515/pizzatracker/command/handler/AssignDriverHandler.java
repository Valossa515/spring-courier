package io.github.valossa515.pizzatracker.command.handler;

import io.github.valossa515.pizzatracker.command.AssignDriverCommand;
import io.github.valossa515.pizzatracker.domain.Driver;
import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.pizzatracker.domain.OrderStatus;
import io.github.valossa515.pizzatracker.notification.DriverAssignedNotification;
import io.github.valossa515.pizzatracker.repository.DriverRepository;
import io.github.valossa515.pizzatracker.repository.OrderRepository;
import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.support.Response;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AssignDriverHandler
        implements CommandHandler<AssignDriverCommand, Response<String>> {

    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final Courier courier;

    public AssignDriverHandler(OrderRepository orderRepository,
                               DriverRepository driverRepository,
                               Courier courier) {
        this.orderRepository = orderRepository;
        this.driverRepository = driverRepository;
        this.courier = courier;
    }

    @Override
    public Response<String> handle(AssignDriverCommand command) {
        Order order = orderRepository.findById(command.getOrderId()).orElse(null);
        if (order == null) {
            return Response.error("Order not found: " + command.getOrderId(), 404);
        }

        if (order.getStatus() != OrderStatus.READY) {
            return Response.error(
                    "Order must be READY to assign a driver. Current: " + order.getStatus(),
                    400
            );
        }

        Driver driver = driverRepository.findById(command.getDriverId()).orElse(null);
        if (driver == null) {
            return Response.error("Driver not found: " + command.getDriverId(), 404);
        }

        if (!driver.isAvailable()) {
            return Response.error("Driver " + driver.getName() + " is not available", 400);
        }

        driver.setAvailable(false);
        order.setDriverId(driver.getId());
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(30));

        orderRepository.save(order);
        driverRepository.save(driver);

        courier.publish(new DriverAssignedNotification(
                order.getId(),
                driver.getId(),
                driver.getName(),
                order.getDeliveryAddress()
        ));

        return Response.success("Driver " + driver.getName()
                + " assigned to order " + order.getId());
    }
}
