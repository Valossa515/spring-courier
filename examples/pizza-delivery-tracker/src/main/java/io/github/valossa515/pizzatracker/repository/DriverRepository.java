package io.github.valossa515.pizzatracker.repository;

import io.github.valossa515.pizzatracker.domain.Driver;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DriverRepository {
    private final Map<String, Driver> drivers = new ConcurrentHashMap<>();

    public Driver save(Driver driver) {
        drivers.put(driver.getId(), driver);
        return driver;
    }

    public Optional<Driver> findById(String id) {
        return Optional.ofNullable(drivers.get(id));
    }

    public List<Driver> findAll() {
        return List.copyOf(drivers.values());
    }

    public Optional<Driver> findFirstAvailable() {
        return drivers.values().stream()
                .filter(Driver::isAvailable)
                .findFirst();
    }
}
