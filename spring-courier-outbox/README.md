# Spring Courier Outbox

Transactional Outbox para entrega confiável de notificações (`INotification`) do
Spring Courier. Grava o evento **na mesma transação** do seu command e o entrega
depois, em background, de forma desacoplada da thread da requisição.

## Quando usar

Sempre que um evento de domínio **precisa** ser publicado se — e somente se — a
transação do command for confirmada (ex.: "pedido criado" só deve disparar
e-mail/integração se o pedido realmente persistiu). O `courier.publish()` comum é
fire-and-forget e não oferece essa garantia atômica.

## Dependência

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier-outbox</artifactId>
    <version>5.0.0</version>
</dependency>
```

Requer um `DataSource` JDBC e `spring-tx`/`spring-jdbc` no contexto (presentes em
qualquer app Spring Boot com banco).

## Schema

A tabela **não** é criada automaticamente em produção. Aplique o DDL fornecido
via Flyway/Liquibase:

- PostgreSQL: [`db/outbox-schema-postgres.sql`](src/main/resources/db/outbox-schema-postgres.sql)
- MySQL 8+: [`db/outbox-schema-mysql.sql`](src/main/resources/db/outbox-schema-mysql.sql)

Em desenvolvimento, `spring.courier.outbox.auto-create-schema=true` cria a tabela
no startup, adaptando o DDL ao banco detectado (H2, PostgreSQL ou MySQL/MariaDB).

## Uso

```java
@Service
public class CreateOrderHandler implements CommandHandler<CreateOrderCommand, Order> {

    private final OrderRepository repository;
    private final OutboxPublisher outbox;

    @Override
    @Transactional
    public Order handle(CreateOrderCommand cmd) {
        Order order = repository.save(new Order(cmd));
        // Persistido na MESMA transação do save acima:
        outbox.publish(new OrderCreatedNotification(order.id()));
        return order;
    }
}
```

Se a transação confirmar, o evento fica gravado; se der rollback, ele some junto.
Um poller em background relê os pendentes e os despacha via `Courier.publish(...)`.

## Garantias

- **Atomicidade:** o evento é gravado na transação do command.
- **At-least-once:** o poller pode reentregar um evento (ex.: crash entre o
  dispatch e a marcação como processado). **Handlers devem ser idempotentes.**
- **Recuperação:** mensagens presas em `PROCESSING` (worker que caiu) voltam a
  `PENDING` após `processing-timeout-ms`.
- **Multi-instância:** o claim é atômico (`PENDING → PROCESSING` condicional), de
  modo que apenas uma instância despacha cada mensagem.

## Configuração

| Propriedade                                   | Default          | Descrição                                              |
|-----------------------------------------------|------------------|--------------------------------------------------------|
| `spring.courier.outbox.enabled`               | `false`          | Liga o módulo.                                         |
| `spring.courier.outbox.poll-delay-ms`         | `5000`           | Intervalo entre ciclos de poll.                        |
| `spring.courier.outbox.batch-size`            | `100`            | Máximo de mensagens despachadas por ciclo.             |
| `spring.courier.outbox.max-attempts`          | `5`              | Tentativas antes de marcar como `FAILED`.              |
| `spring.courier.outbox.table-name`            | `courier_outbox` | Nome da tabela (apenas `[A-Za-z0-9_]`).                |
| `spring.courier.outbox.auto-create-schema`    | `false`          | Cria a tabela no startup (apenas dev).                 |
| `spring.courier.outbox.processing-timeout-ms` | `60000`          | Tempo em `PROCESSING` antes de reentregar.             |

## Customização

Todos os beans são `@ConditionalOnMissingBean` — você pode fornecer seu próprio
`OutboxStore` (ex.: outro banco/dialeto), `OutboxSerializer` ou `OutboxPoller`.
