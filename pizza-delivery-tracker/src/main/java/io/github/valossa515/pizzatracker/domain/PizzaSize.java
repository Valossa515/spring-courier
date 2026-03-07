package io.github.valossa515.pizzatracker.domain;

public enum PizzaSize {
    SMALL(25.0),
    MEDIUM(35.0),
    LARGE(45.0),
    FAMILY(60.0);

    private final double basePrice;

    PizzaSize(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getBasePrice() {
        return basePrice;
    }
}
