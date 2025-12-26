# 📦 Spring Courier  
> 🚀 Uma biblioteca Java para simplificar a implementação do padrão **CQRS + Mediator** em aplicações **Spring Boot**.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.valossa515/spring-courier)](https://central.sonatype.com/artifact/io.github.valossa515/spring-courier)
[![Publish to Maven Central](https://github.com/Valossa515/spring-courier/actions/workflows/publish-maven-central.yml/badge.svg)](https://github.com/Valossa515/spring-courier/actions/workflows/publish-maven-central.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5.x-brightgreen)](https://spring.io/projects/spring-boot)

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

---

## ⚙️ Instalação

Adicione a dependência no seu `pom.xml`:

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier</artifactId>
    <version>0.0.2</version>
</dependency>
```

> 🔧 É necessário ter o **Java 17+** e **Spring Boot 3.x**.

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
