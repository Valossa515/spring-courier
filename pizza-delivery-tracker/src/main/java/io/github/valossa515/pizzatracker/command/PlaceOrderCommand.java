package io.github.valossa515.pizzatracker.command;

import io.github.valossa515.pizzatracker.domain.PizzaFlavor;
import io.github.valossa515.pizzatracker.domain.PizzaSize;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.support.Response;

import java.util.List;

public class PlaceOrderCommand implements ICommand<Response<String>> {
    private final String customerName;
    private final String customerPhone;
    private final String deliveryAddress;
    private final List<PizzaItemDto> items;

    public PlaceOrderCommand(String customerName, String customerPhone,
                             String deliveryAddress, List<PizzaItemDto> items) {
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.items = items;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public List<PizzaItemDto> getItems() {
        return items;
    }

    public record PizzaItemDto(PizzaFlavor flavor, PizzaSize size, int quantity) {
    }
}
