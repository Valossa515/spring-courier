# Copilot Code Review Instructions — Spring Courier

Spring Courier is a Java 17+ library (not an application) that provides CQRS + Mediator pattern infrastructure for Spring Boot 3.x. It is published to Maven Central.

## Critical Rules

- This is a **library** — never approve REST controllers, JPA entities, Docker configs, or auth logic.
- All public exceptions must use the `CourierException` hierarchy. Non-Courier exceptions must produce a generic `"An internal error occurred"` message — never leak internal details.
- Registries (`HandlerRegistry`, `NotificationRegistry`) must be **frozen** after Spring context initialization. Any mutable access post-freeze is a critical bug.
- `volatile boolean frozen` fields are required for cross-thread visibility of freeze state.
- Pipeline depth must remain capped at **64 levels**.
- Method reflection results must be cached (LRU, max 1024 entries).

## Naming Conventions

- Command classes: `{Domain}{Action}Command` (e.g., `CreateProductCommand`)
- Query classes: `{Domain}{Criteria}Query` (e.g., `GetProductByIdQuery`)
- Notifications: `{Domain}{Event}Notification` (e.g., `ProductCreatedNotification`)
- Handler classes: `{Domain}{Action}Handler` (e.g., `CreateProductHandler`)
- Core interfaces use `I` prefix: `IRequest`, `ICommand`, `IQuery`, `INotification`
- Type names: `^[A-Z][a-zA-Z0-9]*$`
- Methods and fields: `^[a-z][a-zA-Z0-9]*$`

## Code Style (Checkstyle enforced)

- Max line length: 120 characters (imports/packages excluded)
- Indentation: 4 spaces, no tabs
- No star imports (`import foo.*`)
- No unused imports
- Always use braces for control structures
- Whitespace around operators and keywords

## Thread Safety

- Registries must use `ConcurrentHashMap` (handlers) and `CopyOnWriteArrayList` (notification handlers).
- Freeze pattern: `volatile boolean frozen` + check at start of mutating methods.
- No mutable shared state without proper synchronization.

## Handler Return Types

Handlers may return: raw value (auto-wrapped in `Response.success()`), `Response<T>` directly, `null` (wrapped as `Response.success(null)`), or `CompletableFuture<T>` (awaited with configurable timeout, default 30s).

## Testing Requirements

- New features must include tests.
- Tests mirror the main package structure under `src/test/java/`.
- Test suffixes: `*Test`, `*AdditionalTest`, `*BranchTest`, `*CoverageTest`, `*FreezeTest`, `*StressTest`.

## Security

- No information disclosure through error messages.
- No injection vulnerabilities (SQL, XSS, command injection).
- No SSRF risks.
- Validate at system boundaries only.

## What to Flag

- Breaking changes to public API without justification.
- Missing tests for new behavior.
- Thread safety violations in registry or pipeline code.
- Internal exception details leaking to `Response.error()`.
- Star imports or Checkstyle violations.
- Mutable access to frozen registries.
