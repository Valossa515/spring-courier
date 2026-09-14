# Spring Courier Cache Redis

Backend **Redis** para o cache de queries e para a idempotência do Spring Courier —
substitui o armazenamento in-memory por um compartilhado entre todas as instâncias.

## Por que existe

Por padrão, `CachingBehavior` e `IdempotencyBehavior` guardam o estado em um
`ConcurrentHashMap` **dentro de cada JVM**. Em produção com múltiplas réplicas isso
significa:

- **Cache:** cada pod tem seu próprio cache → baixa taxa de acerto e resultados
  divergentes entre réplicas.
- **Idempotência:** a deduplicação **não funciona de fato** — o mesmo request
  reenviado cai em outro pod e executa de novo.

Este módulo troca só o *backend*: os behaviors, a API e a configuração continuam iguais.

## Dependência

```xml
<dependency>
    <groupId>io.github.valossa515</groupId>
    <artifactId>spring-courier-cache-redis</artifactId>
    <version>5.0.0</version>
</dependency>
```

Requer **Spring Data Redis** no contexto (`spring-boot-starter-data-redis`) e um
`RedisConnectionFactory` — o que qualquer app Spring Boot com Redis já tem.

## Uso

```properties
# 1) ligue o behavior que você quer (igual a antes)
spring.courier.cache.enabled=true
spring.courier.idempotency.enabled=true

# 2) mande usar Redis como backend
spring.courier.redis.enabled=true
```

Só isso. O core detecta os beans `CacheStore`/`IdempotencyStore` publicados por este
módulo e passa a usá-los no lugar do in-memory. **Nenhuma mudança de código.**

## Configuração

| Propriedade                                    | Default     | Descrição                                              |
|------------------------------------------------|-------------|--------------------------------------------------------|
| `spring.courier.redis.enabled`                 | `false`     | Liga o backend Redis.                                  |
| `spring.courier.redis.key-prefix`              | `courier:`  | Prefixo de todas as chaves (namespacing).              |
| `spring.courier.redis.cache-enabled`           | `true`      | Usa Redis para o cache.                                |
| `spring.courier.redis.idempotency-enabled`     | `true`      | Usa Redis para a idempotência.                         |

As duas últimas permitem misturar: por exemplo, idempotência no Redis e cache em
memória (`cache-enabled=false`).

Chaves finais ficam como `courier:cache:<classe>:<toString>` e
`courier:idem:<classe>:<toString>`.

## Serialização — leia antes de usar

Os valores são serializados pelo `RedisTemplate` configurado. O padrão usa
`GenericJackson2JsonRedisSerializer`, que embute o tipo concreto no JSON para
reconstruir o objeto original.

Consequências:

- **O que o handler retorna precisa ser serializável em JSON pelo Jackson.** Records e
  POJOs comuns funcionam; tipos com construtores privados sem `@JsonCreator`, não.
- `Response` é suportado (o core garante o round-trip via `@JsonCreator`).
- O Redis é tratado como **infraestrutura confiável**: desserialização com informação
  de tipo embutida não deve apontar para uma instância onde terceiros escrevem.

Para trocar a serialização, forneça seu próprio bean chamado `courierRedisTemplate`
(`RedisTemplate<String, Object>`) — a autoconfiguração respeita o seu.

## Garantias e limites

- **TTL** é delegado ao Redis (expiração de chave). Não há `maxSize`: limite o keyspace
  com a política `maxmemory` do Redis.
- **Idempotência distribuída:** um duplicado que chega **depois** do primeiro request
  terminar é servido do Redis em qualquer instância. Dois duplicados **simultâneos em
  instâncias diferentes** ainda podem executar os dois — a deduplicação de requisições
  em voo é por instância.
- **`size()` retorna `-1`**: contar chaves no Redis exige varrer o keyspace, caro demais
  para uma chamada de rotina. `invalidateAll()`/`clear()` usam `SCAN` (nunca `KEYS`),
  em lotes.

## Customização

Todos os beans são `@ConditionalOnMissingBean` — você pode fornecer seu próprio
`CacheStore`, `IdempotencyStore` ou `courierRedisTemplate`.

## Testes

Os testes unitários rodam em qualquer lugar (com `RedisTemplate` mockado). Há também um
teste de integração contra um Redis real em `localhost:6379`, que **se pula sozinho**
quando não há servidor disponível — para rodá-lo, suba um Redis antes:

```bash
redis-server --port 6379 --daemonize yes
./mvnw -pl spring-courier-cache-redis -am test
```
