package io.github.valossa515.pizzatracker.domain;

public class PizzaItem {
    private final PizzaFlavor flavor;
    private final PizzaSize size;
    private final int quantity;

    public PizzaItem(PizzaFlavor flavor, PizzaSize size, int quantity) {
        this.flavor = flavor;
        this.size = size;
        this.quantity = quantity;
    }

    public PizzaFlavor getFlavor() {
        return flavor;
    }

    public PizzaSize getSize() {
        return size;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPrice() {
        return (size.getBasePrice() + flavor.getExtraCost()) * quantity;
    }
}
