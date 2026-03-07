package io.github.valossa515.pizzatracker.controller;

import io.github.valossa515.pizzatracker.command.AssignDriverCommand;
import io.github.valossa515.pizzatracker.command.CancelOrderCommand;
import io.github.valossa515.pizzatracker.command.PlaceOrderCommand;
import io.github.valossa515.pizzatracker.command.UpdateOrderStatusCommand;
import io.github.valossa515.pizzatracker.domain.Order;
import io.github.valossa515.pizzatracker.domain.OrderStatus;
import io.github.valossa515.pizzatracker.query.GetActiveOrdersQuery;
import io.github.valossa515.pizzatracker.query.GetOrderQuery;
import io.github.valossa515.pizzatracker.query.GetOrdersByStatusQuery;
import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.support.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final Courier courier;

    public OrderController(Courier courier) {
        this.courier = courier;
    }

    @PostMapping
    public ResponseEntity<Response<String>> placeOrder(
            @RequestBody PlaceOrderRequest request) {
        PlaceOrderCommand command = new PlaceOrderCommand(
                request.customerName(),
                request.customerPhone(),
                request.deliveryAddress(),
                request.items().stream()
                        .map(i -> new PlaceOrderCommand.PizzaItemDto(
                                i.flavor(), i.size(), i.quantity()))
                        .toList()
        );

        Response<String> response = courier.send(command);
        return response.toEntity();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Response<Order>> getOrder(@PathVariable String orderId) {
        Response<Order> response = courier.send(new GetOrderQuery(orderId));
        return response.toEntity();
    }

    @GetMapping("/active")
    public ResponseEntity<Response<List<Order>>> getActiveOrders() {
        Response<List<Order>> response = courier.send(new GetActiveOrdersQuery());
        return response.toEntity();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Response<List<Order>>> getOrdersByStatus(
            @PathVariable OrderStatus status) {
        Response<List<Order>> response = courier.send(
                new GetOrdersByStatusQuery(status));
        return response.toEntity();
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Response<String>> updateStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus status) {
        Response<String> response = courier.send(
                new UpdateOrderStatusCommand(orderId, status));
        return response.toEntity();
    }

    @PutMapping("/{orderId}/assign-driver")
    public ResponseEntity<Response<String>> assignDriver(
            @PathVariable String orderId,
            @RequestParam String driverId) {
        Response<String> response = courier.send(
                new AssignDriverCommand(orderId, driverId));
        return response.toEntity();
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Response<String>> cancelOrder(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "Customer request") String reason) {
        Response<String> response = courier.send(
                new CancelOrderCommand(orderId, reason));
        return response.toEntity();
    }

    public record PlaceOrderRequest(
            String customerName,
            String customerPhone,
            String deliveryAddress,
            List<PizzaItemRequest> items
    ) {
    }

    public record PizzaItemRequest(
            io.github.valossa515.pizzatracker.domain.PizzaFlavor flavor,
            io.github.valossa515.pizzatracker.domain.PizzaSize size,
            int quantity
    ) {
    }
}
