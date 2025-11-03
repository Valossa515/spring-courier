# 📦 Spring Courier  
> 🚀 Uma biblioteca Java para simplificar a implementação do padrão **CQRS + Mediator** em aplicações **Spring Boot**.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.valossa515/spring-courier)](https://central.sonatype.com/artifact/io.github.valossa515/spring-courier)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-24%2B-orange)](https://openjdk.org/projects/jdk/24/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5.x-brightgreen)](https://spring.io/projects/spring-boot)

---

## 🧠 Sobre

O **Spring Courier** é uma biblioteca leve e extensível que traz para o ecossistema **Spring Boot** a simplicidade e o poder do **MediatR** do .NET.  
Ela fornece uma infraestrutura para desacoplar comandos, consultas e eventos — permitindo aplicações **clean**, **testáveis** e **orientadas a domínio**.

---

## ✨ Recursos

✅ Suporte a **Command Handlers** e **Query Handlers**  
✅ Estrutura genérica e flexível baseada em **interfaces**  
✅ Total integração com o **Spring Context**  
✅ Suporte a **Request/Response Pattern**  
✅ Extensível para eventos e pipelines personalizados  
✅ Zero configuração adicional — **plug and play**

---

## ⚙️ Instalação

Adicione a dependência no seu `pom.xml`:

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier</artifactId>
    <version>0.0.1</version>
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

## 🧩 Estrutura do Projeto

```
spring-courier/
 ├── src/main/java/dev/valossa/springcourier/
 │    ├── core/               # Contratos e abstrações principais
 │    ├── annotations/        # Anotações utilitárias
 │    └── config/             # Configurações da lib
 └── pom.xml
```

---

## 🧱 Integração com Spring Boot

Basta registrar o `Courier` como um Bean no contexto:

```java
@Bean
public Courier courier(ApplicationContext context) {
    return new Courier(context);
}
```

E pronto — todos os `CommandHandler` e `QueryHandler` registrados serão automaticamente resolvidos via IoC do Spring.

---

## 🤝 Contribuindo

Contribuições são **muito bem-vindas**!  
Siga os passos abaixo para contribuir:

1. Faça um fork do repositório  
2. Crie uma branch (`feature/nova-funcionalidade`)  
3. Faça suas alterações e adicione testes  
4. Envie um Pull Request 🚀

---

## 🧾 Licença

Distribuído sob a licença **MIT**.  
Consulte o arquivo [LICENSE](LICENSE) para mais informações.

---

## 💬 Autor

Desenvolvido com ❤️ por [**Felipe Martins**](https://github.com/Valossa515)

---
