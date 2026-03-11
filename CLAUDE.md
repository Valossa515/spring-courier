# CLAUDE.md — Spring Courier

## Project Overview

Spring Courier is a **Java library** (not an application) that provides a **CQRS + Mediator pattern** infrastructure for Spring Boot applications. It is distributed via **Maven Central** and targets Spring Boot 3.x with Java 17+.

- **GroupId:** `io.github.valossa515`
- **ArtifactId:** `spring-courier`
- **Current Version:** 1.3.2
- **License:** MIT

---

## Repository Structure

```
spring-courier/
├── .github/workflows/          # CI/CD pipelines (PR checks, Maven Central publishing)
├── .mvn/wrapper/               # Maven wrapper for consistent builds
├── config/checkstyle/          # Checkstyle code quality rules
├── docs/diagrams/              # PlantUML architecture diagrams
├── src/
│   ├── main/java/io/github/valossa515/spring_courier/
│   │   ├── annotations/        # @EnableSpringCourier, @ExposeHandler
│   │   ├── config/             # CourierAutoConfiguration, CourierProperties
│   │   └── core/
│   │       ├── Courier.java    # Main dispatcher (entry point for users)
│   │       ├── exceptions/     # CourierException, HandlerNotFoundException, ValidationException
│   │       ├── interfaces/     # IRequest, ICommand, IQuery, INotification + handler interfaces
│   │       ├── pipelines/      # PipelineBehavior, PipelineExecutor, PipelineRegistry
│   │       ├── support/        # HandlerRegistry, NotificationRegistry, Response, Discovery PostProcessors
│   │       └── validation/     # ValidationBehavior, Validator, ValidationResult, ValidationError
│   ├── main/resources/
│   │   ├── application.properties
│   │   └── META-INF/spring/   # Spring Boot 3.x autoconfiguration imports
│   └── test/java/...           # 36 test files mirroring main structure
├── pom.xml
├── sonar-project.properties
├── README.md                   # User-facing docs (in Portuguese)
└── CONTRIBUTING.md             # Contributor and release process guide
```

---

## Build System

**Maven** (with wrapper). Always use `./mvnw` to ensure consistent Maven version.

### Common Commands

```bash
# Build and run all tests
./mvnw verify

# Run tests only
./mvnw test

# Build without tests
./mvnw package -DskipTests

# Run checkstyle
./mvnw checkstyle:check

# Generate JaCoCo coverage report (target/site/jacoco/)
./mvnw verify

# Build release artifacts (sources, javadoc, GPG-signed)
./mvnw verify -P release
```

### Key Maven Configuration

- **Java:** 17 (required minimum; compile target is `--release 17`)
- **Spring Boot:** 3.5.5 (provided scope — not bundled in library JAR)
- **Checkstyle:** Enforced; build **fails on violations** (`failOnViolation=true`)
- **JaCoCo:** Generates XML + HTML coverage reports
- **SonarCloud:** Integrated via `sonar-project.properties`

---

## Core Architecture

### Pattern: CQRS + Mediator

The library implements CQRS (Command Query Responsibility Segregation) with a Mediator pattern. The central dispatcher is `Courier`.

### Request Hierarchy

```
IRequest<R>
├── ICommand<R>    — Write operations (side effects)
└── IQuery<R>      — Read operations (no side effects)

INotification      — Fire-and-forget events (no response)
```

### Handler Interfaces

```java
// For commands
CommandHandler<C extends IRequest<R>, R>  → handle(C command): R

// For queries
QueryHandler<Q extends IRequest<R>, R>    → handle(Q query): R

// For notifications (multiple handlers per event)
NotificationHandler<N extends INotification> → handle(N notification): void
```

Handlers are auto-discovered from the Spring context via `BeanPostProcessor` implementations. Any `@Service`, `@Component`, or `@ExposeHandler`-annotated class implementing a handler interface is registered automatically.

### Main Dispatcher: `Courier`

```java
@Autowired Courier courier;

// Send a command or query (synchronous)
Response<MyResponse> result = courier.send(new MyCommand(...));

// Publish a notification (synchronous, all handlers)
courier.publish(new MyEvent(...));

// Publish a notification (async, returns CompletableFuture)
CompletableFuture<Void> future = courier.publishAsync(new MyEvent(...));
```

### Pipeline Behaviors

Cross-cutting concerns are implemented as `PipelineBehavior<R, S>`:

```java
@Component
@Order(1)  // Controls execution order
public class LoggingBehavior implements PipelineBehavior<IRequest<Response<?>>, Response<?>> {
    public Response<?> handle(IRequest<Response<?>> request, Next<Response<?>> next) {
        // Pre-processing
        Response<?> result = next.execute();
        // Post-processing
        return result;
    }
}
```

Pipeline depth is capped at **64 levels** to prevent `StackOverflowError`.

### Response Wrapper

All operations return `Response<T>`:

```java
// Static factory methods
Response.success(data)
Response.success(data, statusCode)
Response.error("message")
Response.error("message", statusCode)

// Conversion to Spring ResponseEntity
ResponseEntity<?> entity = response.toEntity();

// Checks
response.isSuccess()
response.hasData()
response.hasError()
response.getDataOrThrow()  // throws on error
```

---

## Key Conventions

### Naming

| Component        | Convention                           | Example                       |
|-----------------|--------------------------------------|-------------------------------|
| Command class   | `{Domain}{Action}Command`            | `CreateProductCommand`        |
| Query class     | `{Domain}{Criteria}Query`            | `GetProductByIdQuery`         |
| Notification    | `{Domain}{Event}Notification`        | `ProductCreatedNotification`  |
| Handler class   | `{Domain}{Action}Handler`            | `CreateProductHandler`        |
| Handler method  | `handle(...)` or `execute(...)`      | Both are supported            |

### Interfaces Use `I` Prefix

Core contracts follow `I{Name}` convention: `IRequest`, `ICommand`, `IQuery`, `INotification`.

### Error Handling

- `CourierException` (and subclasses) propagate their messages to `Response.error()`.
- All **other** exceptions produce a generic `"An internal error occurred"` message in the response to prevent information leakage.
- Use `HandlerNotFoundException` when a handler is missing.
- Use `ValidationException` for validation failures.

### Thread Safety

- Registries use `ConcurrentHashMap` (handlers) and `CopyOnWriteArrayList` (notification handlers).
- Registries are **frozen** after Spring context initialization (`SmartInitializingSingleton`) — no runtime modifications allowed.
- Method reflection results are cached in an LRU cache (max 1024 entries) using `Collections.synchronizedMap`.
- `volatile boolean frozen` fields ensure cross-thread visibility.

### Handlers Return Types

Handlers may return:
- A raw value (auto-wrapped in `Response.success(value)`)
- A `Response<T>` directly (used as-is)
- `null` (wrapped as `Response.success(null)`)
- `CompletableFuture<T>` (awaited with configurable timeout; default 30 000 ms)

---

## Configuration

### Application Properties

```properties
# Async handler timeout (ms); default 30000; min 100; max 600000
spring.courier.async-timeout-ms=30000
```

### Auto-Configuration

Spring Courier auto-configures via Spring Boot's `AutoConfiguration.imports`. No `@EnableSpringCourier` is needed unless you want explicit control. The following beans are registered (all `@ConditionalOnMissingBean` — user can override):

- `HandlerRegistry`
- `NotificationRegistry`
- `PipelineRegistry`
- `PipelineExecutor`
- `Courier`
- `HandlerDiscoveryPostProcessor`
- `NotificationDiscoveryPostProcessor`
- `BehaviorDiscoveryPostProcessor`

---

## Testing

### Structure

Tests mirror the main package under `src/test/java/`. Test categories:

| Suffix / Location       | Purpose                                     |
|------------------------|---------------------------------------------|
| `*Test.java`           | Standard unit tests                         |
| `*AdditionalTest.java` | Additional edge-case unit tests             |
| `*BranchTest.java`     | Branch-coverage-focused tests               |
| `*CoverageTest.java`   | Coverage gap-filling tests                  |
| `*FreezeTest.java`     | Tests for registry immutability after freeze |
| `*StressTest.java`     | Concurrency / load tests                    |
| `integration/`         | End-to-end Spring context integration tests |

### Running Tests

```bash
./mvnw test                    # All tests
./mvnw test -Dtest=CourierTest # Single class
./mvnw verify                  # Tests + coverage report
```

### Key Test Utilities

- `CourierTestFixture` — Helper for constructing `Courier` instances in tests without full Spring context.

### Coverage

JaCoCo is configured to generate reports at `target/site/jacoco/`. Coverage exclusions apply to `config/`, DTOs, models, and entities.

---

## CI/CD Pipelines

### `.github/workflows/pr-ci.yml`

- **Triggers:** Push to `main`, PRs to `main`
- Runs `mvn -B -ntp verify`
- Runs SonarCloud analysis on `main` branch and non-fork PRs
- Requires secret: `SONAR_TOKEN`

### `.github/workflows/publish-maven-central.yml`

- **Triggers:** GitHub Release published or manual `workflow_dispatch`
- Signs artifacts with GPG (`release` Maven profile)
- Packages sources, javadocs, checksums into Maven Central bundle
- Uploads bundle to Sonatype Central Portal
- Required secrets: `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`

---

## Code Quality

### Checkstyle

- Config: `config/checkstyle/checkstyle.xml`
- **Build fails on any violation** — always run `./mvnw checkstyle:check` before committing.
- Enforced by Maven Checkstyle Plugin 3.6.0.

### SonarCloud

- Project: `Valossa515_spring-courier` on `sonarcloud.io`
- Uses JaCoCo XML coverage and Surefire test results.
- Exclusions: `target/**, build/**, generated-sources/**, out/**`

---

## Annotations

| Annotation           | Purpose                                                                 |
|---------------------|-------------------------------------------------------------------------|
| `@EnableSpringCourier` | Manually import `CourierAutoConfiguration` (not needed with autoconfigure) |
| `@ExposeHandler`     | Mark a class as handler; extends `@Component`; optional bean name param |

---

## Exception Hierarchy

```
RuntimeException
└── CourierException
    ├── HandlerNotFoundException   — No handler registered for request type
    └── ValidationException        — Validation pipeline failure
```

---

## Architecture Diagrams

PlantUML diagrams are located in `docs/diagrams/`. View with any PlantUML-compatible tool (IntelliJ plugin, VS Code extension, or plantuml.com).

| File                             | Description                          |
|----------------------------------|--------------------------------------|
| `architecture-overview.puml`     | 12-step request flow overview        |
| `class-diagram.puml`             | Full class/interface structure       |
| `sequence-diagram-command.puml`  | Command/Query execution flow         |
| `sequence-diagram-notification.puml` | Event publishing flow            |
| `activity-diagram.puml`          | Request processing lifecycle         |
| `component-diagram.puml`         | System component relationships       |
| `deployment-diagram.puml`        | Production deployment topology       |
| `state-diagram.puml`             | Request state machine                |
| `use-case-diagram.puml`          | Developer interaction use cases      |

---

## What This Library Does NOT Include

- No REST controllers or HTTP endpoints (library pattern — users wire `Courier` into their own controllers)
- No JPA entities, repositories, or database migrations
- No Docker/containerization (distributed as JAR via Maven Central)
- No authentication or authorization (application-level concern)

---

## Release Process

See `CONTRIBUTING.md` for the full release checklist. High-level steps:

1. Update version in `pom.xml`
2. Commit and push changes
3. Create a GitHub Release (triggers `publish-maven-central.yml`)
4. Artifacts are signed with GPG and uploaded to Sonatype Central Portal

GPG key and Sonatype credentials must be configured as GitHub repository secrets.
