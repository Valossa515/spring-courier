<p align="center">
  <img src="assets/Spring Courier.png" alt="Spring Courier Logo" width="600"/>
</p>

<h1 align="center">Spring Courier</h1>

<p align="center">
  🚀 A Java library to simplify the implementation of the <strong>CQRS + Mediator</strong> pattern in <strong>Spring Boot</strong> applications.
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.valossa515/spring-courier"><img src="https://img.shields.io/maven-central/v/io.github.valossa515/spring-courier" alt="Maven Central"/></a>
  <a href="https://github.com/Valossa515/spring-courier/actions/workflows/publish-maven-central.yml"><img src="https://github.com/Valossa515/spring-courier/actions/workflows/publish-maven-central.yml/badge.svg" alt="Publish to Maven Central"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License: MIT"/></a>
  <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/java-21%2B-orange" alt="Java"/></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/spring--boot-3.5.x-brightgreen" alt="Spring Boot"/></a>
</p>

---

> 🌐 **Language / Idioma:** 🇧🇷 [Português](README.pt-BR.md) | 🇺🇸 **English** (current)


## 🧠 About

**Spring Courier** is a lightweight and extensible library that brings to the **Spring Boot** ecosystem the simplicity and power of .NET's **MediatR**.
It provides infrastructure to decouple commands, queries, and events — enabling **clean**, **testable**, and **domain-oriented** applications.

---

## ✨ Features

- ✅ **Command Handlers** and **Query Handlers** support
- ✅ **Notification/Event Support** — Publish events to multiple handlers
- ✅ **Validation Pipeline** — Validate requests before execution
- ✅ Generic and flexible structure based on **interfaces**
- ✅ Full integration with the **Spring Context**
- ✅ **Request/Response Pattern** support
- ✅ **Async Support** — Asynchronous publishing with **Virtual Threads** (Java 21)
- ✅ Extensible for custom events and pipelines
- ✅ Zero additional configuration — **plug and play**
- ✅ **Native Slack Alerting** — Alerts directly to Slack without Grafana/Alertmanager
- ✅ **Sealed Exception Hierarchy** — Sealed exception hierarchy for type safety

---

## ☕ Java 21 — What Changed

Starting from version **2.0.0**, Spring Courier requires **Java 21+** (LTS). This update brings significant performance and type safety improvements:

### 🧵 Virtual Threads

`publishAsync()` now uses **Virtual Threads** by default when no custom executor is configured. This replaces the `ForkJoinPool` common pool and offers:

- **Massive scalability** — thousands of async notifications without thread starvation
- **Minimal overhead** — virtual threads are orders of magnitude lighter than platform threads
- **Zero configuration** — works out-of-the-box; you can still provide your own `Executor` if preferred

```java
// Uses virtual threads automatically (Java 21)
courier.publishAsync(new ProductCreatedEvent(id, name));

// Or with a custom executor (optional)
@Bean
public Executor courierAsyncExecutor() {
    return Executors.newFixedThreadPool(10);
}
```

### 🔒 Sealed Exception Hierarchy

The exception hierarchy is now **sealed**, ensuring that only `HandlerNotFoundException` and `ValidationException` extend `CourierException`:

```java
public sealed class CourierException extends RuntimeException
        permits HandlerNotFoundException, ValidationException { }
```

This enables **exhaustive pattern matching** in error handling:

```java
try {
    courier.send(command);
} catch (CourierException ex) {
    switch (ex) {
        case HandlerNotFoundException e -> log.error("Handler not found: {}", e.getMessage());
        case ValidationException e      -> log.warn("Validation failed: {} errors", e.getErrors().size());
    }
}
```

### 🔀 Pattern Matching & Switch Expressions

The library's internal code has been refactored to use Java 21's **pattern matching in switch** and **switch expressions**, making the code more concise and safe.

> ⚠️ **Breaking change:** If your application runs on Java 17, 18, 19, or 20, stay on Spring Courier version **1.x**.

---

## ⚙️ Installation

Add the dependency to your `pom.xml` or `build.gradle`:

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier</artifactId>
    <version>2.0.7</version>
</dependency>
```

```groovy
implementation("io.github.valossa515:spring-courier:2.0.7")
```

> 🔧 Requires **Java 21+** and **Spring Boot 3.x+**.

---

## 🚀 Usage Examples

### 1️⃣ Creating a Command and a Handler

```java
public record CreateProductCommand(String name, BigDecimal price) implements ICommand<CreateProductResponse> {}

@Service
public class CreateProductHandler implements CommandHandler<CreateProductCommand, CreateProductResponse> {

    @Override
    public CreateProductResponse handle(CreateProductCommand command) {
        // Product creation logic
        return new CreateProductResponse(UUID.randomUUID(), command.name(), command.price());
    }
}
```

---

### 2️⃣ Sending the Command with `Courier`

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final Courier courier;

    @PostMapping
    public ResponseEntity<CreateProductResponse> create(@RequestBody CreateProductCommand command) {
        var response = courier.send(command);
        return ResponseEntity.ok(response);
    }
}
```

---

### 3️⃣ Query Example

```java
public record GetProductByIdQuery(UUID id) implements IQuery<GetProductResponse> {}

@Service
public class GetProductByIdHandler implements QueryHandler<GetProductByIdQuery, GetProductResponse> {
    @Override
    public GetProductResponse handle(GetProductByIdQuery query) {
        // Fetch from repository, return DTO
        return new GetProductResponse(query.id(), "Example Product", BigDecimal.valueOf(19.90));
    }
}
```

---

### 4️⃣ Publishing Notifications/Events

```java
// Defining a notification
public record ProductCreatedEvent(UUID productId, String name) implements INotification {}

// Handler 1 - Send email
@Service
public class SendEmailOnProductCreated implements NotificationHandler<ProductCreatedEvent> {
    @Override
    public void handle(ProductCreatedEvent event) {
        // Email sending logic
        System.out.println("Email sent for product: " + event.name());
    }
}

// Handler 2 - Update cache
@Service
public class UpdateCacheOnProductCreated implements NotificationHandler<ProductCreatedEvent> {
    @Override
    public void handle(ProductCreatedEvent event) {
        // Cache update logic
        System.out.println("Cache updated for product: " + event.productId());
    }
}

// Publishing the notification
@Service
public class ProductService {
    private final Courier courier;
    
    public void createProduct(String name) {
        // ... create product ...
        
        // Publish notification - all handlers will be executed
        courier.publish(new ProductCreatedEvent(UUID.randomUUID(), name));
        
        // Or asynchronously
        courier.publishAsync(new ProductCreatedEvent(UUID.randomUUID(), name));
    }
}
```

---

### 5️⃣ Validation with Pipeline Behaviors

```java
// Defining a validator
public class CreateProductValidator implements Validator<CreateProductCommand> {
    @Override
    public ValidationResult validate(CreateProductCommand command) {
        List<ValidationError> errors = new ArrayList<>();
        
        if (command.name() == null || command.name().isEmpty()) {
            errors.add(new ValidationError("name", "Name cannot be empty"));
        }
        
        if (command.price() == null || command.price().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationError("price", "Price must be greater than zero"));
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}

// Registering the validation behavior
@Bean
public ValidationBehavior<CreateProductCommand, CreateProductResponse> productValidationBehavior() {
    return new ValidationBehavior<>(List.of(new CreateProductValidator()));
}
```

---

## 📈 Observability (Micrometer / Prometheus / Grafana)

Starting from version **1.4.0**, Spring Courier features **optional** instrumentation with [Micrometer](https://micrometer.io/), allowing you to export metrics to Prometheus, Grafana, and other backends.

### Activation

Simply add `micrometer-core` (or `spring-boot-starter-actuator`) to your application's classpath. Spring Courier automatically detects it and replaces the default `Courier` with an instrumented `MeteredCourier`.

```xml
<!-- If your application already uses Actuator, nothing else is needed -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

To disable metrics:

```properties
spring.courier.metrics.enabled=false
```

### Available Metrics

#### ⏱️ Timers (latency with p50, p95, p99 percentiles)

| Metric                           | Description                                | Tags                                            |
|----------------------------------|--------------------------------------------|-------------------------------------------------|
| `courier.send.duration`          | Command/query execution time               | `request.type`, `request.category`, `outcome`   |
| `courier.publish.duration`       | Notification publish time                  | `notification.type`                             |
| `courier.publish.async.duration` | Async notification publish time            | `notification.type`                             |

#### 🔢 Counters (throughput and errors)

| Metric                         | Description                             | Tags                               |
|--------------------------------|-----------------------------------------|------------------------------------|
| `courier.send`                 | Total dispatched requests               | `request.type`, `outcome`          |
| `courier.publish`              | Total published notifications           | `notification.type`                |
| `courier.handler.errors`       | Handler errors (excludes validation)    | `request.type`, `exception.type`   |
| `courier.handler.timeouts`     | Async handler timeouts                  | —                                  |
| `courier.validation.failures`  | Pipeline validation failures            | `request.type`                     |

> **Note:** `handler.errors` and `validation.failures` are mutually exclusive — a failed request increments one or the other, never both.

#### 📊 Gauges (registry state)

| Metric                                     | Description                                         |
|--------------------------------------------|-----------------------------------------------------|
| `courier.handlers.registered`              | Number of registered command/query handlers          |
| `courier.notification.handlers.registered` | Number of registered notification handlers           |
| `courier.pipeline.behaviors.registered`    | Number of registered pipeline behaviors              |

#### 🏷️ Tags

| Tag                  | Possible Values                        | Description                       |
|----------------------|----------------------------------------|-----------------------------------|
| `request.type`       | Simple class name of the request       | E.g., `CreateProductCommand`      |
| `request.category`   | `command`, `query`, `request`          | Request type (CQRS)               |
| `notification.type`  | Simple class name of the notification  | E.g., `ProductCreatedEvent`       |
| `outcome`            | `success`, `error`                     | Operation result                  |
| `exception.type`     | Simple class name of the exception     | Captured exception type           |

### Example with Prometheus + Grafana

1. Add `micrometer-registry-prometheus` to your application:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. Expose the metrics endpoint in `application.properties`:

```properties
management.endpoints.web.exposure.include=prometheus,health,metrics
management.metrics.export.prometheus.enabled=true
spring.courier.metrics.enabled=true
```

3. Configure Prometheus to scrape the `/actuator/prometheus` endpoint.

4. Import the Grafana dashboard available at [`docs/grafana/courier-dashboard.json`](docs/grafana/courier-dashboard.json).

> 📖 See the [PromQL Guide](docs/grafana/PROMQL_REFERENCE.md) for ready-to-use monitoring queries.

### 🔔 Slack Alerts

#### Via Grafana (External)

Starting from version **1.6.0**, Spring Courier includes ready-made configurations for **Slack** alerts via **Grafana Unified Alerting**. The alerts are based on metrics exported by Micrometer and collected by Prometheus.

1. Create a [Slack Incoming Webhook](https://api.slack.com/messaging/webhooks) for the desired channel
2. Copy the provisioning files from `docs/grafana/provisioning/` to the Grafana provisioning directory
3. Restart Grafana

> 📖 See the [complete Slack Alerting Guide](docs/grafana/SLACK_ALERTING.md) for detailed setup instructions.

#### 🆕 Via Native Spring Courier (without Grafana)

Starting from version **1.7.0**, Spring Courier provides **native Slack alerts** — no need for Grafana, Prometheus Alertmanager, or any external alerting infrastructure. Just configure the webhook and the library automatically evaluates metrics, sending notifications directly to Slack.

**Activation:**

Add the configuration to `application.properties`:

```properties
# Required — enables native alerting
spring.courier.slack.webhook-url=https://hooks.slack.com/services/T.../B.../xxx

# Optional — Slack channel (overrides the webhook default)
spring.courier.slack.channel=#courier-alerts

# Optional — application name in alerts (default: Spring Courier)
spring.courier.slack.app-name=my-api
```

> ⚠️ For native alerting to work, Micrometer (`spring-boot-starter-actuator`) must be on the classpath and `spring.courier.metrics.enabled=true` (enabled by default).

**Available Alerts:**

| Alert | Condition | Severity |
|-------|-----------|----------|
| **High Error Ratio** | Error ratio > 5% | `warning` |
| **High p99 Latency** | p99 send > 1s | `warning` |
| **Handler Timeouts** | Timeouts detected | `critical` |
| **Validation Spike** | Validation failures > 10/s | `warning` |
| **Throughput Drop** | Throughput drop > 50% | `critical` |

**Advanced Settings:**

```properties
# Rule evaluation interval (10–3600s, default: 60s)
spring.courier.slack.evaluation-interval-seconds=60

# Cooldown between repeated alerts of the same type (1–1440min, default: 15min)
spring.courier.slack.cooldown-minutes=15

# Minimum time the condition must hold before firing (default: 300s)
spring.courier.slack.for-duration-seconds=300

# Customizable thresholds
spring.courier.slack.thresholds.error-ratio=0.05
spring.courier.slack.thresholds.p99-latency-seconds=1.0
spring.courier.slack.thresholds.validation-rate=10.0
spring.courier.slack.thresholds.throughput-drop-ratio=0.5
```

**Alert lifecycle:** `OK → PENDING → FIRING → RESOLVED`

- **PENDING:** The condition was detected, but has not yet reached the minimum time (`for-duration-seconds`)
- **FIRING:** The condition held and a notification was sent to Slack (with cooldown between re-notifications)
- **RESOLVED:** The condition normalized — a resolution message is automatically sent

**To disable:**

```properties
spring.courier.slack.enabled=false
```

---

## 🧩 Project Structure

```
spring-courier/
 ├── src/main/java/io/github/valossa515/spring_courier/
 │    ├── core/               # Core contracts and abstractions
 │    ├── annotations/        # Utility annotations
 │    └── config/             # Library configurations
 ├── docs/diagrams/           # UML architecture diagrams
 └── pom.xml
```

---

## 📊 Diagrams and Architecture

To better understand the architecture and workings of the library, check the **UML diagrams** available in the [`docs/diagrams/`](docs/diagrams/) folder:

- 🎯 **[Architecture Overview](docs/diagrams/architecture-overview.puml)** - Simplified main flow diagram
- 🏗️ **[Class Diagram](docs/diagrams/class-diagram.puml)** - Class and interface structure
- 🔄 **[Sequence Diagram - Command/Query](docs/diagrams/sequence-diagram-command.puml)** - Command and query execution flow
- 📢 **[Sequence Diagram - Notifications](docs/diagrams/sequence-diagram-notification.puml)** - Event publishing
- 📋 **[Activity Diagram](docs/diagrams/activity-diagram.puml)** - Request processing flow
- 👤 **[Use Case Diagram](docs/diagrams/use-case-diagram.puml)** - Features and use cases
- 🧩 **[Component Diagram](docs/diagrams/component-diagram.puml)** - Component architecture
- 🚀 **[Deployment Diagram](docs/diagrams/deployment-diagram.puml)** - Deployment structure
- 🔀 **[State Diagram](docs/diagrams/state-diagram.puml)** - Request lifecycle

> 💡 Diagrams are in PlantUML format. See the [diagrams README](docs/diagrams/README.md) for viewing instructions.

---

## 🧱 Spring Boot Integration

Spring Courier has **auto-configuration** enabled by default. Just add the dependency and all handlers will be automatically discovered and registered.

All `CommandHandler`, `QueryHandler`, and `NotificationHandler` classes annotated with `@Service` or `@Component` are automatically registered via Spring's IoC container.

---

## 🤝 Contributing

Contributions are **very welcome**!
Follow the steps below to contribute:

1. Fork the repository
2. Create a branch (`feature/new-feature`)
3. Make your changes and add tests
4. Submit a Pull Request 🚀

For more information about the contribution process and release publishing, see [CONTRIBUTING.md](CONTRIBUTING.md).

---

## 📦 Releases and Publishing

This project uses **GitHub Actions** to fully automate the publishing process to Maven Central.

### For Users

Published versions are available on [Maven Central](https://central.sonatype.com/artifact/io.github.valossa515/spring-courier).

### For Maintainers

To publish a new version:

1. **Update the version** in `pom.xml`
2. **Create a release** on GitHub with the corresponding tag (e.g., `v1.0.0`)
3. The **GitHub Action will be triggered automatically** and publish the library to Maven Central

The action performs:
- ✅ Build and tests
- ✅ JAR generation (main, sources, javadocs)
- ✅ GPG signing of all artifacts
- ✅ Checksum generation (SHA1, MD5, SHA256, SHA512)
- ✅ Maven Central bundle creation
- ✅ Automatic upload to Sonatype Central Portal

**Required secrets**: See [CONTRIBUTING.md](CONTRIBUTING.md#-release-process-for-maintainers) for detailed instructions on configuring GitHub secrets.

---

## 🧾 License

Distributed under the **MIT** license.
See the [LICENSE](LICENSE) file for more information.

---

## 💬 Author

Made with ❤️ by [**Felipe Martins**](https://github.com/Valossa515)
