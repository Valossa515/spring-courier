# Awesome Lists — Submission Kit

Ready-to-submit entries for the curated GitHub "awesome" lists most relevant to Spring Courier. Each section contains the exact markdown line to add, the target file/section in the upstream repo, and the PR title/body template.

> ⚠️ Read the `CONTRIBUTING.md` of each list before opening a PR — most require alphabetical ordering and a one-line description following a specific format.

---

## 1. akullpp/awesome-java

- **Repo:** https://github.com/akullpp/awesome-java
- **Target file:** `README.md`
- **Target section:** `### Microservice` (or `### Reactive`/`### Utility` depending on reviewer feedback)
- **Insertion order:** alphabetical by project name

**Markdown line to add:**

```markdown
* [Spring Courier](https://github.com/Valossa515/spring-courier) - Lightweight MediatR-style CQRS + Mediator library for Spring Boot, with built-in retry, cache, idempotency, OpenTelemetry, and Micrometer metrics.
```

**PR title:**
```
Add Spring Courier
```

**PR body template:**
```markdown
Adds **Spring Courier**, a lightweight CQRS + Mediator library for Spring Boot.

- **Repository:** https://github.com/Valossa515/spring-courier
- **License:** MIT
- **Stars:** <current count>
- **Maven Central:** published (`io.github.valossa515:spring-courier`)
- **Activity:** actively maintained, released to Maven Central with GitHub Actions

Brings the MediatR developer experience to Spring Boot with zero boilerplate, plus production-grade behaviors (retry, cache, idempotency, validation, OpenTelemetry tracing, Micrometer metrics, native Slack alerts).

Checked the contribution guidelines:
- [x] Entry is alphabetically sorted
- [x] Project is older than 30 days and actively maintained
- [x] One-line description, no marketing fluff
- [x] License is OSI-approved (MIT)
```

---

## 2. ulisesbocchio/awesome-spring

- **Repo:** https://github.com/ulisesbocchio/awesome-spring
- **Target file:** `README.md`
- **Target section:** `## Libraries` → `### Architecture` (or `### Reactive` if Architecture absent)
- **Insertion order:** alphabetical

**Markdown line to add:**

```markdown
- [Spring Courier](https://github.com/Valossa515/spring-courier) - MediatR-style CQRS + Mediator for Spring Boot 3.x/4.x with Java 21 virtual threads, retry, cache, idempotency, and native Slack alerts.
```

**PR title:**
```
Add Spring Courier to Architecture/Libraries
```

---

## 3. meirwah/awesome-workflow-engines (CQRS/event flow audience)

> Less perfect fit but reaches the CQRS audience. Submit only if there's a "Java" or "Libraries" subsection.

- **Repo:** https://github.com/meirwah/awesome-workflow-engines
- **Decision:** SKIP unless reviewer feedback in #1/#2 suggests a better fit. The closer audience match is **awesome-spring** above.

---

## 4. ddd-crew / awesome-domain-driven-design

- **Repo:** https://github.com/heynickc/awesome-ddd
- **Target file:** `readme.md`
- **Target section:** `### Frameworks & Libraries` → look for `Java` subsection
- **Insertion order:** alphabetical

**Markdown line to add:**

```markdown
- [Spring Courier](https://github.com/Valossa515/spring-courier) - Java 21 CQRS + Mediator library for Spring Boot. Decouples commands, queries and domain events with zero boilerplate.
```

**PR title:**
```
Add Spring Courier (Java CQRS/Mediator library)
```

---

## 5. tjarvstrand/awesome-spring-boot (community list)

- **Repo:** https://github.com/hantsy/awesome-spring-boot-cn (PT-BR/EN community) or any active fork with recent commits
- **Action:** check fork last commit < 90 days before submitting
- **Markdown line:** same as #2

---

## Submission Order & Strategy

| # | List                      | Priority | Why                                                  |
|---|---------------------------|----------|------------------------------------------------------|
| 1 | akullpp/awesome-java      | 🔥 High  | ~50k stars, strongest discovery signal               |
| 2 | ulisesbocchio/awesome-spring | 🔥 High | Direct audience match                              |
| 3 | heynickc/awesome-ddd      | Medium   | Niche but aligned (CQRS readers care about DDD)      |
| 4 | Niche/fork lists          | Low      | Wait for #1–#2 results first                         |

**Tips:**
- Submit one PR at a time, wait 1–2 weeks before the next, to avoid looking spammy.
- Reply to maintainer feedback within 24h — these PRs get closed fast if abandoned.
- If rejected, ask politely which section/criteria would make it accept-worthy.
- Don't open more than 2 awesome-list PRs in the same week from the same account.

---

## Pre-Submission Checklist

Before opening any PR:

- [ ] README has badges (Maven Central, license, build, stars) ✅ done
- [ ] README has a clear "Why" / value proposition ✅ done
- [ ] README has Quick Start in <60s ✅ done
- [ ] Project has 10+ stars (boosts maintainer confidence — ask close contacts to star first)
- [ ] License file present (MIT) ✅ done
- [ ] CI green on `main` ✅ done
- [ ] Last commit within 30 days ✅ done
- [ ] Maven Central artifact resolvable ✅ done
