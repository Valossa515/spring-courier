# I built a MediatR alternative for Spring Boot in Java 21 — here's what I learned

> Cover image suggestion: split-screen "Before / After" code from the README, or the Spring Courier logo over a dark background.
>
> Tags: `#java` `#spring` `#cqrs` `#opensource`
> Canonical URL: set this to the eventual blog post on your own site (improves SEO without penalizing dev.to).

---

If you've ever written .NET, you probably know the feeling: you reach for `MediatR`, define a `Command`, a `Handler`, call `_mediator.Send(...)`, and you're done. No registration boilerplate, no service locator gymnastics, no abstract base classes — just CQRS the way it should be.

Coming back to **Spring Boot** I kept asking myself: *why don't we have this?*

We have heavyweight options like **Axon Framework** (event sourcing, sagas, the whole CQRS-on-rails experience) and we have **Spring Modulith** (great for module boundaries inside a monolith). What I wanted was the *middle ground*: the MediatR developer experience, on Spring, with nothing extra to learn.

So I built it. It's called **[Spring Courier](https://github.com/Valossa515/spring-courier)** and after 3 months on Maven Central it's been downloaded 13.8k times across 87 organizations. This post is a brain-dump of what I learned along the way.

## The problem with most Spring controllers

Look familiar?

```java
@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;
    private final ProductValidator validator;
    private final AuditLogger auditor;

    @PostMapping("/products")
    public ResponseEntity<Product> create(@RequestBody CreateProductRequest req) {
        validator.validate(req);            // boilerplate
        auditor.log("create", req);         // boilerplate
        Product saved = service.create(req);// the only line that matters
        return ResponseEntity.status(201).body(saved);
    }
}
```

Three injected collaborators, three lines of plumbing, one line of real work. Multiply this across every endpoint in a real application and you've built a maintenance problem.

## The MediatR-style controller

This is the same controller with a Mediator:

```java
@RestController
@RequiredArgsConstructor
public class ProductController {
    private final Courier courier;

    @PostMapping("/products")
    public ResponseEntity<?> create(@RequestBody CreateProductCommand cmd) {
        return courier.send(cmd).toEntity();
    }
}
```

Validation, auditing, logging, retry, caching, idempotency — all of that lives in **pipeline behaviors** that wrap every request. The controller goes back to doing one thing: translating HTTP into a command and a command into HTTP.

## How small can it be?

The whole "Hello, Courier" example is three things:

**1.** Add the dependency:

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier</artifactId>
    <version>4.0.0</version>
</dependency>
```

**2.** Define a command and its handler:

```java
public record CreateProductCommand(String name, BigDecimal price)
        implements ICommand<Product> {}

@Service
public class CreateProductHandler
        implements CommandHandler<CreateProductCommand, Product> {
    public Product handle(CreateProductCommand cmd) {
        return new Product(UUID.randomUUID(), cmd.name(), cmd.price());
    }
}
```

**3.** Dispatch:

```java
Response<Product> response = courier.send(new CreateProductCommand("Book", BigDecimal.TEN));
```

No `@EnableSpringCourier`, no `WebConfig`, no registration. The library walks the Spring context at startup, finds every `CommandHandler`/`QueryHandler`/`NotificationHandler`, and indexes them by request type.

## Three things I learned the hard way

### 1. Virtual threads aren't a free lunch — but they're close

When Java 21 dropped, my first instinct was "just replace the executor with `Executors.newVirtualThreadPerTaskExecutor()` and call it a day". And mostly that worked. But two surprises:

- **`ThreadLocal` propagation** breaks naturally if you don't think about it. I had to introduce a `CourierContext` carrying correlation ID, and explicitly capture/restore it when crossing the virtual-thread boundary. The fix is straightforward; the bug surface if you don't is invisible.
- **Pinned threads from `synchronized`** still exist in the libraries you depend on. The fix in user code is `ReentrantLock`. The fix in *transitive dependencies* is "wait for them to upgrade."

Net result for Spring Courier: virtual threads are the default for `publishAsync()` and `sendAllAsync()`, and the context propagation is handled automatically.

### 2. Sealed exception hierarchies make pattern matching beautiful

```java
public sealed class CourierException extends RuntimeException
        permits HandlerNotFoundException, ValidationException { }
```

Combined with Java 21's pattern matching in `switch`:

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

No `default` branch. The compiler proves exhaustiveness. Adding a new exception subtype is a compile error everywhere you switch on it — exactly what you want.

### 3. "Optional features" are a packaging problem

I shipped Micrometer integration, OpenTelemetry tracing, native Slack alerts, GraalVM hints — all in the same JAR, all activated automatically *if and only if* their dependencies are on the classpath.

The trick is **conditional auto-configuration**:

```java
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(prefix = "spring.courier.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CourierMetricsAutoConfiguration { ... }
```

The library declares everything as `optional` in `pom.xml`. Users get a 150 KB JAR with zero forced transitive dependencies. They opt in by adding the libraries they already wanted anyway.

This is the single most important thing I'd recommend to anyone writing a Spring Boot library: **never force a dependency on your user**.

## When (not) to use it

Honest comparison:

| You should pick…    | When…                                                                         |
|---------------------|-------------------------------------------------------------------------------|
| **Spring Courier**  | You want the MediatR dev experience on Spring with minimal footprint          |
| **Axon Framework**  | You need event sourcing, sagas, distributed CQRS at the architectural level   |
| **Spring Modulith** | You're building a modular monolith and want enforced module boundaries        |
| **Nothing**         | Your controllers genuinely have one line of code and no cross-cutting concerns |

Spring Courier is intentionally *not* an event-sourcing framework, *not* a saga orchestrator, and *not* a replacement for Spring's existing event publisher (which it actually complements).

## What's next

The 3.0 release added retry behavior, in-memory query caching, `@Idempotent`, OpenTelemetry tracing and native Slack alerting. The roadmap is mostly community-driven now — if there's a behavior you wish existed, open an issue.

## Try it

- **GitHub:** https://github.com/Valossa515/spring-courier
- **Maven Central:** https://central.sonatype.com/artifact/io.github.valossa515/spring-courier
- **Docs:** linked from the README

If this resonates, a ⭐ on the repo really helps the project get discovered. Issues, PRs and feedback are all welcome — this started as a personal project and I'd love to see where the community takes it.

What's the cross-cutting concern *you* wish Spring Boot handled out of the box? Drop it in the comments.
