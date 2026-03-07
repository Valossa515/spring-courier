package io.github.valossa515.pizzatracker.validation;

import io.github.valossa515.pizzatracker.command.PlaceOrderCommand;
import io.github.valossa515.spring_courier.core.validation.ValidationError;
import io.github.valossa515.spring_courier.core.validation.ValidationResult;
import io.github.valossa515.spring_courier.core.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PlaceOrderValidator implements Validator<PlaceOrderCommand> {

    @Override
    public ValidationResult validate(PlaceOrderCommand request) {
        List<ValidationError> errors = new ArrayList<>();

        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            errors.add(new ValidationError("customerName", "Customer name is required"));
        }

        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
            errors.add(new ValidationError("deliveryAddress", "Delivery address is required"));
        }

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            errors.add(new ValidationError("customerPhone", "Customer phone is required"));
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            errors.add(new ValidationError("items", "At least one pizza item is required"));
        } else {
            for (int i = 0; i < request.getItems().size(); i++) {
                var item = request.getItems().get(i);
                if (item.quantity() <= 0) {
                    errors.add(new ValidationError(
                            "items[" + i + "].quantity",
                            "Quantity must be greater than 0"
                    ));
                }
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }
}
