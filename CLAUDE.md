# CLAUDE.md — Spring Courier

## Project Overview

Spring Courier is a **Java library** (not an application) that provides a **CQRS + Mediator pattern** infrastructure for Spring Boot applications. It is distributed via **Maven Central**, built against the Spring Boot 4.x BOM (compatible with 3.x and 4.x apps), and requires Java 21+.

- **GroupId:** `io.github.valossa515`
- **ArtifactId:** `spring-courier`
- **Current Version:** 3.0.0
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
│   │   ├── annotations/        # @EnableSpringCourier, @ExposeHandler, @Idempotent, @Timeout
│   │   ├── config/             # CourierAutoConfiguration, CourierProperties + actuator/metrics/
│   │   │                       #   slack/tracing/transaction/validation autoconfigurations
│   │   └── core/
│   │       ├── Courier.java    # Main dispatcher (entry point for users)
│   │       ├── exceptions/     # CourierException, HandlerNotFoundException, ValidationException
│   │       ├── interfaces/     # IRequest, ICommand, IQuery, INotification + handler interfaces
│   │       ├── metrics/        # CourierMetrics, MeteredCourier (Micrometer integration)
│   │       ├── pipelines/      # PipelineBehavior/Executor/Registry + built-in behaviors
│   │       │                   #   (Logging, Validation, Caching, Retry, Idempotency,
│   │       │                   #    Transaction, Tracing) + Pre/PostProcessor support
│   │       ├── slack/          # SlackNotifier, SlackAlertManager (metric-based alerting)
│   │       ├── support/        # HandlerRegistry, NotificationRegistry, Response,
│   │       │                   #   CourierContext(Holder), Discovery PostProcessors
│   │       ├── testing/        # CourierTestSupport (user-facing test helper)
│   │       └── validation/     # ValidationBehavior, JakartaValidationBehavior, Validator
│   ├── main/resources/
│   │   ├── application.properties
│   │   └── META-INF/spring/   # Spring Boot autoconfiguration imports
│   └── test/java/...           # 70+ test files mirroring main structure
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

- **Java:** 21 (required minimum; compile target is `--release 21`)
- **Spring Boot:** 4.0.6 (provided scope — not bundled in library JAR)
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

Built-in behaviors and their orders (lower = outermost):

| Behavior                   | Order                        | Enabled by                                    |
|----------------------------|------------------------------|-----------------------------------------------|
| `LoggingBehavior`          | `HIGHEST_PRECEDENCE`         | default on (`spring.courier.logging.enabled`) |
| `IdempotencyBehavior`      | `HIGHEST_PRECEDENCE + 5`     | `spring.courier.idempotency.enabled`          |
| `TracingBehavior`          | `HIGHEST_PRECEDENCE + 10`    | OpenTelemetry on classpath                    |
| `CachingBehavior`          | `HIGHEST_PRECEDENCE + 50`    | `spring.courier.cache.enabled`                |
| `JakartaValidationBehavior`| `HIGHEST_PRECEDENCE + 100`   | jakarta.validation on classpath               |
| `RetryBehavior`            | `HIGHEST_PRECEDENCE + 150`   | `spring.courier.retry.enabled`                |
| `TransactionBehavior`      | `HIGHEST_PRECEDENCE + 200`   | spring-tx on classpath                        |

Retry runs **outside** the transaction so each attempt gets a fresh
transaction. Caching/idempotency only store **successful** results and require
the request type to override `toString()` (records qualify).

### Handler Execution Model

- Handlers run **inline on the calling thread** — Spring transactions,
  security context, and MDC apply to the handler.
- Handler exceptions propagate through the behavior chain (rollback, retry,
  and `IRequestExceptionHandler`s see them) and are converted to an error
  `Response` at the dispatcher boundary; `send()` never rethrows them.
- `@Timeout`-annotated requests are offloaded to a virtual thread with a
  watchdog (returns 504 on expiry). Thread-bound state does **not** propagate
  in this mode, so `@Timeout` should not be combined with `TransactionBehavior`.
- `spring.courier.async-timeout-ms` applies to handlers returning
  `CompletableFuture`.

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
# Applies to CompletableFuture-returning handlers; @Timeout-annotated
# requests use their annotation value instead
spring.courier.async-timeout-ms=30000

# Notification publishing: SEQUENTIAL | PARALLEL_WHEN_ALL | STOP_ON_FIRST_ERROR
spring.courier.notification-strategy=SEQUENTIAL

# Built-in behaviors (see groups: logging, cache, retry, idempotency,
# tracing, metrics, slack in CourierProperties)
spring.courier.logging.enabled=true
spring.courier.cache.enabled=false          # + ttl-seconds, max-size
spring.courier.retry.enabled=false          # + max-attempts, delay-ms, multiplier
spring.courier.idempotency.enabled=false    # + max-size
spring.courier.slack.webhook-url=           # enables Slack alerting when set
```

### Auto-Configuration

Spring Courier auto-configures via Spring Boot's `AutoConfiguration.imports`. No `@EnableSpringCourier` is needed unless you want explicit control. The following beans are registered (all `@ConditionalOnMissingBean` — user can override):

- `HandlerRegistry`
- `NotificationRegistry`
- `PipelineRegistry`
- `ProcessorRegistry`
- `PipelineExecutor`
- `Courier`
- `ResponseEntityConverter`
- `HandlerDiscoveryPostProcessor`
- `NotificationDiscoveryPostProcessor`
- `BehaviorDiscoveryPostProcessor`
- `ProcessorDiscoveryPostProcessor`
- Built-in behaviors (Logging, Caching, Retry, Idempotency) per their
  `spring.courier.*` toggles

Additional autoconfigurations activate conditionally: actuator endpoint,
Micrometer metrics (`MeteredCourier`), OpenTelemetry tracing, Slack alerting,
Jakarta validation, and `TransactionBehavior` (when spring-tx is present).

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
| `@Idempotent`        | Deduplicate requests via `IdempotencyBehavior`; `ttlSeconds` (default 3600) |
| `@Timeout`           | Off-thread execution with watchdog timeout (ms) for a request type      |

---

## Exception Hierarchy

```
RuntimeException
└── CourierException (sealed)
    ├── HandlerNotFoundException (final)  — No handler registered for request type
    └── ValidationException (final)      — Validation pipeline failure
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
