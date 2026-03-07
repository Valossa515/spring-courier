package io.github.valossa515.pizzatracker.domain;

public class Driver {
    private final String id;
    private final String name;
    private final String phone;
    private final String vehiclePlate;
    private boolean available;

    public Driver(String id, String name, String phone, String vehiclePlate) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.vehiclePlate = vehiclePlate;
        this.available = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
