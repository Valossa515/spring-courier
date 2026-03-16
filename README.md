<p align="center">
  <img src="assets/Spring Courier.png" alt="Spring Courier Logo" width="600"/>
</p>

<h1 align="center">Spring Courier</h1>

<p align="center">
  🚀 Uma biblioteca Java para simplificar a implementação do padrão <strong>CQRS + Mediator</strong> em aplicações <strong>Spring Boot</strong>.
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.valossa515/spring-courier"><img src="https://img.shields.io/maven-central/v/io.github.valossa515/spring-courier" alt="Maven Central"/></a>
  <a href="https://github.com/Valossa515/spring-courier/actions/workflows/publish-maven-central.yml"><img src="https://github.com/Valossa515/spring-courier/actions/workflows/publish-maven-central.yml/badge.svg" alt="Publish to Maven Central"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License: MIT"/></a>
  <a href="https://openjdk.org/projects/jdk/17/"><img src="https://img.shields.io/badge/java-17%2B-orange" alt="Java"/></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/spring--boot-3.5.x-brightgreen" alt="Spring Boot"/></a>
</p>

---

## 🧠 Sobre

O **Spring Courier** é uma biblioteca leve e extensível que traz para o ecossistema **Spring Boot** a simplicidade e o poder do **MediatR** do .NET.  
Ela fornece uma infraestrutura para desacoplar comandos, consultas e eventos — permitindo aplicações **clean**, **testáveis** e **orientadas a domínio**.

---

## ✨ Recursos

✅ Suporte a **Command Handlers** e **Query Handlers**  
✅ **Notification/Event Support** — Publique eventos para múltiplos handlers  
✅ **Validation Pipeline** — Valide requests antes da execução  
✅ Estrutura genérica e flexível baseada em **interfaces**  
✅ Total integração com o **Spring Context**  
✅ Suporte a **Request/Response Pattern**  
✅ **Async Support** — Publicação assíncrona de notificações  
✅ Extensível para eventos e pipelines personalizados  
✅ Zero configuração adicional — **plug and play**  
✅ **Slack Alerting Nativo** — Alertas direto no Slack sem Grafana/Alertmanager

---

## ⚙️ Instalação

Adicione a dependência no seu `pom.xml` ou `build.gradle`:

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier</artifactId>
    <version>1.7.2</version>
</dependency>
```

```groovy
implementation("io.github.valossa515:spring-courier:1.7.2")
```

> 🔧 É necessário ter o **Java 17+** e **Spring Boot 3.x+**.

---

## 🚀 Exemplo de Uso

### 1️⃣ Criando um Command e um Handler

```java
public record CreateProductCommand(String name, BigDecimal price) implements ICommand<CreateProductResponse> {}

@Service
public class CreateProductHandler implements CommandHandler<CreateProductCommand, CreateProductResponse> {

    @Override
    public CreateProductResponse handle(CreateProductCommand command) {
        // Lógica de criação do produto
        return new CreateProductResponse(UUID.randomUUID(), command.name(), command.price());
    }
}
```

---

### 2️⃣ Enviando o Command com o `Courier`

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

### 3️⃣ Exemplo de Query

```java
public record GetProductByIdQuery(UUID id) implements IQuery<GetProductResponse> {}

@Service
public class GetProductByIdHandler implements QueryHandler<GetProductByIdQuery, GetProductResponse> {
    @Override
    public GetProductResponse handle(GetProductByIdQuery query) {
        // Buscar no repositório, retornar DTO
        return new GetProductResponse(query.id(), "Example Product", BigDecimal.valueOf(19.90));
    }
}
```

---

### 4️⃣ Publicando Notificações/Eventos

```java
// Definindo uma notificação
public record ProductCreatedEvent(UUID productId, String name) implements INotification {}

// Handler 1 - Enviar email
@Service
public class SendEmailOnProductCreated implements NotificationHandler<ProductCreatedEvent> {
    @Override
    public void handle(ProductCreatedEvent event) {
        // Lógica para enviar email
        System.out.println("Email enviado para produto: " + event.name());
    }
}

// Handler 2 - Atualizar cache
@Service
public class UpdateCacheOnProductCreated implements NotificationHandler<ProductCreatedEvent> {
    @Override
    public void handle(ProductCreatedEvent event) {
        // Lógica para atualizar cache
        System.out.println("Cache atualizado para produto: " + event.productId());
    }
}

// Publicando a notificação
@Service
public class ProductService {
    private final Courier courier;
    
    public void createProduct(String name) {
        // ... criar produto ...
        
        // Publica notificação - todos os handlers serão executados
        courier.publish(new ProductCreatedEvent(UUID.randomUUID(), name));
        
        // Ou de forma assíncrona
        courier.publishAsync(new ProductCreatedEvent(UUID.randomUUID(), name));
    }
}
```

---

### 5️⃣ Validação com Pipeline Behaviors

```java
// Definindo um validador
public class CreateProductValidator implements Validator<CreateProductCommand> {
    @Override
    public ValidationResult validate(CreateProductCommand command) {
        List<ValidationError> errors = new ArrayList<>();
        
        if (command.name() == null || command.name().isEmpty()) {
            errors.add(new ValidationError("name", "Nome não pode ser vazio"));
        }
        
        if (command.price() == null || command.price().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationError("price", "Preço deve ser maior que zero"));
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}

// Registrando o behavior de validação
@Bean
public ValidationBehavior<CreateProductCommand, CreateProductResponse> productValidationBehavior() {
    return new ValidationBehavior<>(List.of(new CreateProductValidator()));
}
```

---

## 📈 Observabilidade (Micrometer / Prometheus / Grafana)

A partir da versão **1.4.0**, o Spring Courier possui instrumentação **opcional** com [Micrometer](https://micrometer.io/), permitindo exportar métricas para Prometheus, Grafana e outros backends.

### Ativação

Basta adicionar o `micrometer-core` (ou `spring-boot-starter-actuator`) ao classpath da sua aplicação. O Spring Courier detecta automaticamente e substitui o `Courier` padrão por um `MeteredCourier` instrumentado.

```xml
<!-- Se sua aplicação já usa Actuator, não precisa de mais nada -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Para desabilitar as métricas:

```properties
spring.courier.metrics.enabled=false
```

### Métricas Disponíveis

#### ⏱️ Timers (latência com percentis p50, p95, p99)

| Métrica                          | Descrição                                  | Tags                                            |
|----------------------------------|--------------------------------------------|------------------------------------------------|
| `courier.send.duration`          | Tempo de execução de commands/queries      | `request.type`, `request.category`, `outcome`  |
| `courier.publish.duration`       | Tempo de publicação de notificações        | `notification.type`                            |
| `courier.publish.async.duration` | Tempo de publicação assíncrona             | `notification.type`                            |

#### 🔢 Counters (throughput e erros)

| Métrica                        | Descrição                               | Tags                               |
|--------------------------------|-----------------------------------------|------------------------------------|
| `courier.send`                 | Total de requests despachados           | `request.type`, `outcome`          |
| `courier.publish`              | Total de notificações publicadas        | `notification.type`                |
| `courier.handler.errors`       | Erros em handlers                       | `request.type`, `exception.type`   |
| `courier.handler.timeouts`     | Timeouts de handlers async              | —                                  |
| `courier.validation.failures`  | Falhas de validação no pipeline         | `request.type`                     |

#### 📊 Gauges (estado do registro)

| Métrica                                    | Descrição                                      |
|--------------------------------------------|------------------------------------------------|
| `courier.handlers.registered`              | Quantidade de command/query handlers registrados|
| `courier.notification.handlers.registered` | Quantidade de notification handlers registrados |
| `courier.pipeline.behaviors.registered`    | Quantidade de pipeline behaviors registrados    |

#### 🏷️ Tags

| Tag                  | Valores possíveis                   | Descrição                         |
|----------------------|-------------------------------------|-----------------------------------|
| `request.type`       | Nome simples da classe do request   | Ex: `CreateProductCommand`        |
| `request.category`   | `command`, `query`, `request`       | Tipo do request (CQRS)            |
| `notification.type`  | Nome simples da classe da notificação| Ex: `ProductCreatedEvent`        |
| `outcome`            | `success`, `error`                  | Resultado da operação             |
| `exception.type`     | Nome simples da exceção             | Tipo da exceção capturada         |

### Exemplo com Prometheus + Grafana

1. Adicione `micrometer-registry-prometheus` à sua aplicação:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. Exponha o endpoint de métricas no `application.properties`:

```properties
management.endpoints.web.exposure.include=prometheus,health,metrics
management.metrics.export.prometheus.enabled=true
spring.courier.metrics.enabled=true
```

3. Configure o Prometheus para fazer scrape do endpoint `/actuator/prometheus`.

4. Importe o dashboard Grafana disponível em [`docs/grafana/courier-dashboard.json`](docs/grafana/courier-dashboard.json).

> 📖 Consulte o [Guia de PromQL](docs/grafana/PROMQL_REFERENCE.md) para queries prontas para monitoramento.

### 🔔 Alertas no Slack

#### Via Grafana (externa)

A partir da versão **1.6.0**, o Spring Courier inclui configurações prontas para alertas no **Slack** via **Grafana Unified Alerting**. Os alertas são baseados nas métricas exportadas pelo Micrometer e coletadas pelo Prometheus.

1. Crie um [Slack Incoming Webhook](https://api.slack.com/messaging/webhooks) para o canal desejado
2. Copie os arquivos de provisioning de `docs/grafana/provisioning/` para o diretório de provisioning do Grafana
3. Reinicie o Grafana

> 📖 Consulte o [Guia completo de Slack Alerting](docs/grafana/SLACK_ALERTING.md) para instruções detalhadas de configuração.

#### 🆕 Via Spring Courier Nativo (sem Grafana)

A partir da versão **1.7.0**, o Spring Courier traz **alertas nativos no Slack** — sem necessidade de Grafana, Prometheus Alertmanager ou qualquer infraestrutura de alerting externa. Basta configurar o webhook e a biblioteca avalia as métricas automaticamente, enviando notificações direto para o Slack.

**Ativação:**

Adicione a configuração ao `application.properties`:

```properties
# Obrigatório — ativa o alerting nativo
spring.courier.slack.webhook-url=https://hooks.slack.com/services/T.../B.../xxx

# Opcional — canal Slack (sobrescreve o default do webhook)
spring.courier.slack.channel=#courier-alerts

# Opcional — nome da aplicação nos alertas (default: Spring Courier)
spring.courier.slack.app-name=minha-api
```

> ⚠️ Para o alerting nativo funcionar, o Micrometer (`spring-boot-starter-actuator`) precisa estar no classpath e `spring.courier.metrics.enabled=true` (habilitado por padrão).

**Alertas Disponíveis:**

| Alerta | Condição | Severidade |
|--------|----------|------------|
| **High Error Ratio** | Error ratio > 5% | `warning` |
| **High p99 Latency** | p99 send > 1s | `warning` |
| **Handler Timeouts** | Timeouts detectados | `critical` |
| **Validation Spike** | Falhas de validação > 10/s | `warning` |
| **Throughput Drop** | Queda de throughput > 50% | `critical` |

**Configurações Avançadas:**

```properties
# Intervalo de avaliação das regras (10–3600s, default: 60s)
spring.courier.slack.evaluation-interval-seconds=60

# Cooldown entre alertas repetidos do mesmo tipo (1–1440min, default: 15min)
spring.courier.slack.cooldown-minutes=15

# Tempo mínimo que a condição deve se manter antes de disparar (default: 300s)
spring.courier.slack.for-duration-seconds=300

# Thresholds personalizáveis
spring.courier.slack.thresholds.error-ratio=0.05
spring.courier.slack.thresholds.p99-latency-seconds=1.0
spring.courier.slack.thresholds.validation-rate=10.0
spring.courier.slack.thresholds.throughput-drop-ratio=0.5
```

**Ciclo de vida dos alertas:** `OK → PENDING → FIRING → RESOLVED`

- **PENDING:** A condição foi detectada, mas ainda não atingiu o tempo mínimo (`for-duration-seconds`)
- **FIRING:** A condição se manteve e uma notificação foi enviada ao Slack (com cooldown entre re-notificações)
- **RESOLVED:** A condição normalizou — uma mensagem de resolução é enviada automaticamente

**Para desabilitar:**

```properties
spring.courier.slack.enabled=false
```

---

## 🧩 Estrutura do Projeto

```
spring-courier/
 ├── src/main/java/dev/valossa/springcourier/
 │    ├── core/               # Contratos e abstrações principais
 │    ├── annotations/        # Anotações utilitárias
 │    └── config/             # Configurações da lib
 ├── docs/diagrams/           # Diagramas UML da arquitetura
 └── pom.xml
```

---

## 📊 Diagramas e Arquitetura

Para entender melhor a arquitetura e funcionamento da biblioteca, consulte os **diagramas UML** disponíveis na pasta [`docs/diagrams/`](docs/diagrams/):

- 🎯 **[Visão Geral da Arquitetura](docs/diagrams/architecture-overview.puml)** - Diagrama simplificado do fluxo principal
- 🏗️ **[Diagrama de Classes](docs/diagrams/class-diagram.puml)** - Estrutura de classes e interfaces
- 🔄 **[Diagrama de Sequência - Command/Query](docs/diagrams/sequence-diagram-command.puml)** - Fluxo de execução de commands e queries
- 📢 **[Diagrama de Sequência - Notificações](docs/diagrams/sequence-diagram-notification.puml)** - Publicação de eventos
- 📋 **[Diagrama de Atividades](docs/diagrams/activity-diagram.puml)** - Fluxo de processamento de requests
- 👤 **[Diagrama de Casos de Uso](docs/diagrams/use-case-diagram.puml)** - Funcionalidades e casos de uso
- 🧩 **[Diagrama de Componentes](docs/diagrams/component-diagram.puml)** - Arquitetura de componentes
- 🚀 **[Diagrama de Implantação](docs/diagrams/deployment-diagram.puml)** - Estrutura de deployment
- 🔀 **[Diagrama de Estados](docs/diagrams/state-diagram.puml)** - Ciclo de vida de requests

> 💡 Os diagramas estão em formato PlantUML. Veja o [README dos diagramas](docs/diagrams/README.md) para instruções de visualização.

---

## 🧱 Integração com Spring Boot

O Spring Courier possui **auto-configuração** habilitada por padrão. Basta adicionar a dependência e todos os handlers serão automaticamente descobertos e registrados.

Todos os `CommandHandler`, `QueryHandler` e `NotificationHandler` anotados com `@Service` ou `@Component` são automaticamente registrados via IoC do Spring.

---

## 🤝 Contribuindo

Contribuições são **muito bem-vindas**!  
Siga os passos abaixo para contribuir:

1. Faça um fork do repositório
2. Crie uma branch (`feature/nova-funcionalidade`)
3. Faça suas alterações e adicione testes
4. Envie um Pull Request 🚀

Para mais informações sobre o processo de contribuição e publicação de releases, consulte o [CONTRIBUTING.md](CONTRIBUTING.md).

---

## 📦 Releases e Publicação

Este projeto utiliza **GitHub Actions** para automatizar completamente o processo de publicação no Maven Central.

### Para Usuários

As versões publicadas estão disponíveis no [Maven Central](https://central.sonatype.com/artifact/io.github.valossa515/spring-courier).

### Para Maintainers

Para publicar uma nova versão:

1. **Atualize a versão** no `pom.xml`
2. **Crie uma release** no GitHub com a tag correspondente (ex: `v1.0.0`)
3. A **GitHub Action será disparada automaticamente** e publicará a biblioteca no Maven Central

A action executa:
- ✅ Build e testes
- ✅ Geração de JARs (main, sources, javadocs)
- ✅ Assinatura GPG de todos os artefatos
- ✅ Geração de checksums (SHA1, MD5, SHA256, SHA512)
- ✅ Criação do bundle Maven Central
- ✅ Upload automático para o Sonatype Central Portal

**Secrets necessários**: Consulte o [CONTRIBUTING.md](CONTRIBUTING.md#-processo-de-release-para-maintainers) para instruções detalhadas sobre a configuração dos secrets do GitHub.

---

## 🧾 Licença

Distribuído sob a licença **MIT**.  
Consulte o arquivo [LICENSE](LICENSE) para mais informações.

---

## 💬 Autor

Desenvolvido com ❤️ por [**Felipe Martins**](https://github.com/Valossa515)

---