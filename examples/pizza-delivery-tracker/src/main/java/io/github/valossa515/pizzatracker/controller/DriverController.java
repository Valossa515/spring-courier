package io.github.valossa515.pizzatracker.controller;

import io.github.valossa515.pizzatracker.domain.Driver;
import io.github.valossa515.pizzatracker.repository.DriverRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverRepository driverRepository;

    public DriverController(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @PostMapping
    public ResponseEntity<Driver> registerDriver(@RequestBody DriverRequest request) {
        String id = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Driver driver = new Driver(id, request.name(), request.phone(), request.vehiclePlate());
        driverRepository.save(driver);
        return ResponseEntity.status(201).body(driver);
    }

    @GetMapping
    public ResponseEntity<List<Driver>> listDrivers() {
        return ResponseEntity.ok(driverRepository.findAll());
    }

    public record DriverRequest(String name, String phone, String vehiclePlate) {
    }
}
