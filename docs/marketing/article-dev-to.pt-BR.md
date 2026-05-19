# Criei uma alternativa ao MediatR para Spring Boot em Java 21 — o que aprendi no caminho

> Sugestão de capa: split-screen do "Antes / Depois" do README, ou o logo do Spring Courier sobre fundo escuro.
>
> Tags: `#java` `#spring` `#cqrs` `#opensource` `#braziliandevs`
> Canonical URL: aponte para a versão definitiva no seu blog (melhora SEO sem penalizar o dev.to).

---

Se você já mexeu com .NET, provavelmente conhece a sensação: você importa o `MediatR`, define um `Command`, um `Handler`, chama `_mediator.Send(...)` e acabou. Sem boilerplate de registro, sem ginástica de service locator, sem classe base abstrata — só CQRS do jeito que deveria ser.

Voltando ao **Spring Boot** eu ficava me perguntando: *por que a gente não tem isso?*

Temos opções pesadas como o **Axon Framework** (event sourcing, sagas, CQRS-on-rails completo) e temos o **Spring Modulith** (ótimo para fronteiras de módulos dentro de um monolito). O que eu queria era o *meio do caminho*: a experiência de dev do MediatR, em cima do Spring, sem nada extra para aprender.

Então construí. Chama-se **[Spring Courier](https://github.com/Valossa515/spring-courier)** e depois de 3 meses no Maven Central já foi baixado 13,8 mil vezes por 87 organizações distintas. Esse post é um despejo do que aprendi no processo.

## O problema com a maioria dos controllers Spring

Familiar?

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
        Product saved = service.create(req);// a única linha que importa
        return ResponseEntity.status(201).body(saved);
    }
}
```

Três colaboradores injetados, três linhas de plumbing, uma linha de trabalho real. Multiplica isso por todos os endpoints de uma aplicação real e pronto: você construiu um problema de manutenção.

## O controller no estilo MediatR

O mesmo controller com um Mediator:

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

Validação, auditoria, logging, retry, cache, idempotência — tudo isso mora em **pipeline behaviors** que envelopam cada request. O controller volta a fazer uma coisa só: traduzir HTTP em command e command em HTTP.

## Quão pequeno isso pode ser?

O "Olá, Courier" inteiro são três passos:

**1.** Adicione a dependência:

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier</artifactId>
    <version>3.0.0</version>
</dependency>
```

**2.** Defina um command e seu handler:

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

**3.** Despache:

```java
Response<Product> response = courier.send(new CreateProductCommand("Book", BigDecimal.TEN));
```

Sem `@EnableSpringCourier`, sem `WebConfig`, sem registro. A biblioteca varre o contexto Spring no startup, encontra todo `CommandHandler`/`QueryHandler`/`NotificationHandler` e indexa por tipo de request.

## Três coisas que aprendi do jeito difícil

### 1. Virtual threads não são almoço grátis — mas chegam perto

Quando o Java 21 saiu, meu primeiro instinto foi "troca o executor por `Executors.newVirtualThreadPerTaskExecutor()` e tá feito". E quase deu certo. Duas surpresas:

- **Propagação de `ThreadLocal`** quebra naturalmente se você não pensar nisso. Tive que introduzir um `CourierContext` carregando o correlation ID e capturar/restaurar explicitamente ao cruzar o boundary da virtual thread. O fix é simples; a superfície de bug se você não fizer é invisível.
- **Pinned threads vindo de `synchronized`** ainda existem nas libs das quais você depende. No seu código a solução é `ReentrantLock`. Nas *dependências transitivas* a solução é "esperar elas atualizarem".

Resultado para o Spring Courier: virtual threads são o padrão em `publishAsync()` e `sendAllAsync()`, e a propagação de contexto é automática.

### 2. Hierarquia selada de exceções deixa pattern matching lindo

```java
public sealed class CourierException extends RuntimeException
        permits HandlerNotFoundException, ValidationException { }
```

Combinado com pattern matching em `switch` do Java 21:

```java
try {
    courier.send(command);
} catch (CourierException ex) {
    switch (ex) {
        case HandlerNotFoundException e -> log.error("Handler não encontrado: {}", e.getMessage());
        case ValidationException e      -> log.warn("Validação falhou: {} erros", e.getErrors().size());
    }
}
```

Sem `default`. O compilador prova exaustividade. Adicionar um novo subtipo de exceção vira erro de compilação em todo lugar que faz `switch` — exatamente o que você quer.

### 3. "Feature opcional" é um problema de empacotamento

Mandei integração com Micrometer, tracing OpenTelemetry, alertas nativos no Slack, hints GraalVM — tudo no mesmo JAR, tudo ativado automaticamente *se e somente se* as dependências respectivas estiverem no classpath.

O truque é **auto-configuração condicional**:

```java
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(prefix = "spring.courier.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CourierMetricsAutoConfiguration { ... }
```

A biblioteca declara tudo como `optional` no `pom.xml`. O usuário recebe um JAR de 150 KB com zero dependência transitiva forçada. Ele opta por adicionar as libs que ele já queria de qualquer jeito.

Essa é a única coisa que eu mais recomendaria para quem escreve biblioteca Spring Boot: **nunca force uma dependência no seu usuário**.

## Quando (não) usar

Comparação honesta:

| Escolha…            | Quando…                                                                          |
|---------------------|----------------------------------------------------------------------------------|
| **Spring Courier**  | Você quer a experiência do MediatR no Spring com footprint mínimo                |
| **Axon Framework**  | Você precisa de event sourcing, sagas, CQRS distribuído no nível arquitetural    |
| **Spring Modulith** | Você está construindo um monolito modular com fronteiras de módulo aplicadas     |
| **Nada**            | Seus controllers genuinamente têm uma linha de código e zero concern transversal |

Spring Courier intencionalmente **não** é framework de event sourcing, **não** é orquestrador de saga e **não** substitui o publisher de eventos do Spring (na verdade complementa).

## Próximos passos

A release 3.0 trouxe retry behavior, cache em memória para queries, `@Idempotent`, tracing OpenTelemetry e alertas nativos no Slack. O roadmap agora é majoritariamente community-driven — se tem algum behavior que você gostaria que existisse, abre uma issue.

## Experimenta

- **GitHub:** https://github.com/Valossa515/spring-courier
- **Maven Central:** https://central.sonatype.com/artifact/io.github.valossa515/spring-courier
- **Docs:** linkadas a partir do README

Se isso fez sentido pra você, uma ⭐ no repo ajuda muito o projeto a ser descoberto. Issues, PRs e feedback são todos bem-vindos — começou como projeto pessoal e adoraria ver pra onde a comunidade leva.

Qual é o concern transversal que *você* queria que o Spring Boot resolvesse direto? Deixa nos comentários.
