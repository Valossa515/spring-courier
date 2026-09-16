# Spring Courier Resilience

**Circuit breaker**, **rate limiter** e **bulkhead** como pipeline behaviors, sobre
[Resilience4j](https://resilience4j.readme.io/).

## Por que existe

O core já entrega `RetryBehavior`. Retry **sem** circuit breaker é um risco conhecido:
quando uma dependência cai, cada request vira N chamadas contra o serviço que já está
falhando — você multiplica a carga exatamente no pior momento e prolonga o incidente.

Este módulo fecha essa lacuna e adiciona os outros dois controles clássicos de carga.

## Dependência

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier-resilience</artifactId>
    <version>13.0.0</version>
</dependency>
```

O Resilience4j vem junto (é detalhe de implementação do módulo, não algo que se espera
que sua aplicação já tenha).

## Uso

```properties
spring.courier.resilience.enabled=true
```

Só isso liga o **circuit breaker**. Rate limiter e bulkhead são **opt-in** — veja a
seção de configuração para o porquê.

Nenhuma mudança de código: são behaviors, descobertos e encaixados no pipeline como
qualquer outro.

## Ordem no pipeline — a parte que importa

| Ordem | Behavior | Onde |
|------:|----------|------|
| +100 | `JakartaValidationBehavior` | *core* |
| **+130** | **`RateLimiterBehavior`** | este módulo |
| **+140** | **`CircuitBreakerBehavior`** | este módulo |
| +150 | `RetryBehavior` | *core* |
| **+160** | **`BulkheadBehavior`** | este módulo |
| +200 | `TransactionBehavior` | *core* |

Menor = mais externo. Três decisões deliberadas:

- **Breaker por FORA do retry.** Com o circuito aberto, a chamada é rejeitada **uma vez**
  em vez de ser repetida N vezes. É o ponto central do módulo: retry contra dependência
  caída amplifica a falha. O custo é o breaker contabilizar uma falha por *request* e não
  por *tentativa* — troca intencional, já que o objetivo é aliviar carga, não medir
  tentativas. Há um teste (`CircuitBreakerRetryOrderingTest`) que roda as duas ordenações
  lado a lado e fixa essa diferença.
- **Rate limiter antes do breaker, depois da validação.** Request inválido não consome
  quota, e o corte acontece o mais cedo que o pipeline permite.
- **Bulkhead por DENTRO do retry.** A permissão é adquirida por tentativa e liberada antes
  do backoff dormir. Segurar uma vaga durante o `sleep` do retry esfomearia o pool com
  chamadas que não estão fazendo trabalho nenhum.

## Configuração por tipo de request

As instâncias são resolvidas pelo **nome simples da classe do request**, então cada tipo
tem seu próprio circuito/budget/pool e é ajustável pela configuração padrão do
Resilience4j:

```properties
resilience4j.circuitbreaker.instances.CreateOrderCommand.failure-rate-threshold=50
resilience4j.ratelimiter.instances.GetProductByIdQuery.limit-for-period=100
resilience4j.bulkhead.instances.GenerateReportQuery.max-concurrent-calls=5
```

Se sua aplicação já usa o starter do Resilience4j, os registries dela são reaproveitados
(todos os beans são `@ConditionalOnMissingBean`).

## Switches do módulo

| Propriedade | Default | Descrição |
|-------------|---------|-----------|
| `spring.courier.resilience.enabled` | `false` | Liga o módulo. |
| `spring.courier.resilience.circuit-breaker-enabled` | `true` | Circuit breaker. |
| `spring.courier.resilience.rate-limiter-enabled` | `false` | Rate limiter. |
| `spring.courier.resilience.bulkhead-enabled` | `false` | Bulkhead. |

**Por que só o breaker liga por padrão:** ele só age depois que algo já está falhando —
não tem como estrangular um sistema saudável. Rate limiter e bulkhead impõem **teto
rígido**, e os defaults do Resilience4j são propositalmente pequenos; ligá-los por baixo
dos panos limitaria sua vazão silenciosamente. São escolha consciente, com limite
configurado.

## Respostas em caso de rejeição

Rejeição vira status HTTP com significado, e não um `500` genérico:

| Situação | Resposta |
|----------|----------|
| Circuito aberto | `503` — *circuit is open* |
| Rate limit esgotado | `429` — *rate limit exhausted* |
| Bulkhead saturado | `503` — *too many concurrent executions* |

Isso é feito por um `IRequestExceptionHandler` registrado pelo módulo. Vale saber o
porquê: o core **mascara** a mensagem de qualquer exceção que não seja `CourierException`
(proteção contra vazamento de informação), e esse tipo é `sealed` — um módulo não
consegue entrar na hierarquia. O handler é o ponto de extensão que o próprio core oferece
para esse caso, então a proteção continua intacta e o chamador ainda recebe um status
acionável. `503` e `429` dizem "tente de novo mais tarde"; um `500` mascarado não diz nada.

## Métricas

Quando o Micrometer está presente, o módulo conta rejeições (tag `request.type`):

- `courier.circuitbreaker.rejected`
- `courier.ratelimiter.rejected`
- `courier.bulkhead.rejected`
