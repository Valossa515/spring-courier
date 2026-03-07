package io.github.valossa515.pizzatracker.domain;

public enum PizzaFlavor {
    MARGHERITA(0.0),
    PEPPERONI(5.0),
    FOUR_CHEESE(7.0),
    HAWAIIAN(6.0),
    MEAT_LOVERS(8.0),
    VEGGIE(4.0),
    BBQ_CHICKEN(7.0),
    PORTUGUESE(6.0);

    private final double extraCost;

    PizzaFlavor(double extraCost) {
        this.extraCost = extraCost;
    }

    public double getExtraCost() {
        return extraCost;
    }
}
