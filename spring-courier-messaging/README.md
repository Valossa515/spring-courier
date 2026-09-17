# Spring Courier Messaging

Entrega os eventos do **outbox** em um broker (**Kafka**), para que outros serviços
possam consumi-los.

## Por que existe

O `spring-courier-outbox` resolve a atomicidade: o evento é gravado na mesma transação
do command e entregue depois. Mas ele entrega **em processo** — o poller relê os
pendentes e chama `Courier.publish(...)`, ou seja, os handlers rodam dentro da **mesma
aplicação** que gerou o evento.

Isso é exatamente o que você quer num monólito. Num sistema distribuído, não: o evento
precisa **sair** da aplicação. Este módulo troca o destino da entrega sem mexer em nada
do que já está escrito — nem no `OutboxPublisher`, nem nos handlers, nem no schema.

## Dependência

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier-messaging</artifactId>
    <version>14.0.0</version>
</dependency>
```

O `spring-kafka` é `provided`: sua aplicação já configura Kafka (bootstrap servers,
segurança, serializers), então é ela que traz a dependência — mesma escolha do módulo
Redis.

## Uso

```properties
spring.courier.outbox.enabled=true
spring.courier.messaging.enabled=true
spring.kafka.bootstrap-servers=localhost:9092
```

Nenhuma mudança de código. O lado produtor continua idêntico:

```java
@Override
@Transactional
public Order handle(CreateOrderCommand cmd) {
    Order order = repository.save(new Order(cmd));
    outbox.publish(new OrderCreatedNotification(order.id()));   // igual a antes
    return order;
}
```

A diferença é para onde o poller entrega: em vez de `Courier.publish(...)` em processo,
o evento vira um registro no tópico.

## Como funciona

O módulo `spring-courier-outbox` expõe uma SPI:

```java
@FunctionalInterface
public interface OutboxDispatcher {
    void dispatch(OutboxMessage message);
}
```

O default (`CourierOutboxDispatcher`) desserializa e republica em processo. Como o bean é
`@ConditionalOnMissingBean`, basta este módulo registrar o `KafkaOutboxDispatcher` para
assumir o lugar dele. **Ligar este módulo redireciona a entrega — não a duplica.**

O dispatcher recebe a `OutboxMessage` crua, e não o evento desserializado. Isso é
deliberado: o payload JSON já está gravado na linha do outbox, então ele é encaminhado
**como está**. Sem round trip de desserializar/reserializar, e o serviço consumidor não
precisa das classes de evento do produtor no classpath.

## Anatomia do registro

| Parte | Conteúdo |
|-------|----------|
| Tópico | `topic-prefix` + nome simples da classe do evento (`courier.OrderCreatedNotification`) |
| Key | o id do outbox |
| Value | o payload JSON gravado, sem alteração |
| Header `courier-message-id` | o id do outbox |
| Header `courier-event-type` | a classe original do evento (`com.example.OrderCreatedNotification`) |

**Sobre a key:** o id do outbox distribui a carga bem entre as partições, mas **não
garante ordem por agregado**. Se a ordem por entidade importa, forneça seu próprio
`OutboxTopicResolver` e/ou `OutboxDispatcher`.

**Sobre o `courier-message-id`:** a entrega é *at-least-once* (o poller pode reentregar
após um crash), então o consumidor precisa de um id estável para deduplicar. É para isso
que o header existe.

## As duas decisões que sustentam a garantia

**1. O send é aguardado.** `dispatch()` bloqueia até o broker confirmar. O poller só
marca a mensagem como `PROCESSED` depois que `dispatch()` retorna — retornar assim que o
send foi enfileirado permitiria marcar como entregue algo que o Kafka ainda não aceitou,
e um crash nessa janela perderia o evento. Exatamente a falha que o outbox existe para
evitar. Falha no envio vira exceção, o poller contabiliza a tentativa e a mensagem
continua pendente para a próxima rodada.

**2. O producer é durável por padrão:** `acks=all` e produtor idempotente. Confirmar só
no líder entregaria a garantia de durabilidade no último passo; a idempotência impede que
um retry do próprio producer transforme um evento em duas duplicatas no tópico.

## Configuração

| Propriedade | Default | Descrição |
|-------------|---------|-----------|
| `spring.courier.messaging.enabled` | `false` | Liga o módulo. |
| `spring.courier.messaging.topic-prefix` | `courier.` | Prefixo do tópico derivado do evento. |
| `spring.courier.messaging.topic` | *(vazio)* | Se preenchido, **tudo** vai para este tópico e o prefixo é ignorado. |
| `spring.courier.messaging.send-timeout-ms` | `10000` | Espera máxima pelo ack do broker. |
| `spring.courier.messaging.bootstrap-servers` | `${spring.kafka.bootstrap-servers}` | Só se o outbox precisar de um cluster diferente do resto da aplicação. |

O `send-timeout-ms` limita quanto tempo **uma** mensagem pode travar um ciclo de poll —
vale considerar isso junto com `spring.courier.outbox.batch-size`.

## Customização

Todos os beans são `@ConditionalOnMissingBean`:

- **Tópico por evento:** forneça um `OutboxTopicResolver`.
- **Producer próprio** (segurança, Avro, outro cluster): forneça um bean chamado
  `courierKafkaTemplate` (`KafkaTemplate<String, String>`). O nome é distinto de propósito
  para não colidir com o `kafkaTemplate` da sua aplicação.
- **Outro broker** (RabbitMQ, SNS...): forneça seu próprio `OutboxDispatcher` — esta é a
  mesma porta que este módulo usa.

## Consumindo do outro lado

O consumidor não precisa de nada deste projeto: é um tópico Kafka comum.

```java
@KafkaListener(topics = "courier.OrderCreatedNotification", groupId = "billing")
public void on(ConsumerRecord<String, String> record) {
    String messageId = new String(
            record.headers().lastHeader("courier-message-id").value(), UTF_8);
    if (alreadyProcessed(messageId)) {
        return;   // at-least-once: deduplicar é responsabilidade do consumidor
    }
    // record.value() é o JSON do evento
}
```
