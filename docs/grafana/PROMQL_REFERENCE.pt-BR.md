> 🌐 **Language / Idioma:** 🇧🇷 **Português** (atual) | 🇺🇸 [English](PROMQL_REFERENCE.md)

# PromQL Reference — Spring Courier Metrics

Guia de consultas PromQL prontas para monitoramento do Spring Courier com Prometheus e Grafana.

> **Nota:** O Micrometer converte automaticamente os nomes das métricas de dot-notation (`courier.send.duration`) para o formato Prometheus com underscores (`courier_send_duration_seconds`).

---

## 📬 Throughput

### Taxa de requests (send) por segundo

```promql
sum(rate(courier_send_total[$__rate_interval]))
```

### Taxa de requests por tipo

```promql
sum by(request_type) (rate(courier_send_total[$__rate_interval]))
```

### Taxa de requests por categoria (command vs query)

```promql
sum by(request_category) (rate(courier_send_duration_seconds_count[$__rate_interval]))
```

### Taxa de notificações publicadas por segundo

```promql
sum(rate(courier_publish_total[$__rate_interval]))
```

### Taxa de notificações por tipo

```promql
sum by(notification_type) (rate(courier_publish_total[$__rate_interval]))
```

---

## ⏱️ Latência

### p50 / p95 / p99 de `send()`

```promql
# p50
histogram_quantile(0.50, sum by(le) (rate(courier_send_duration_seconds_bucket[$__rate_interval])))

# p95
histogram_quantile(0.95, sum by(le) (rate(courier_send_duration_seconds_bucket[$__rate_interval])))

# p99
histogram_quantile(0.99, sum by(le) (rate(courier_send_duration_seconds_bucket[$__rate_interval])))
```

### p95 de `send()` por tipo de request

```promql
histogram_quantile(0.95, sum by(le, request_type) (rate(courier_send_duration_seconds_bucket[$__rate_interval])))
```

### p95 de `publish()` (notificações síncronas)

```promql
histogram_quantile(0.95, sum by(le) (rate(courier_publish_duration_seconds_bucket[$__rate_interval])))
```

### p95 de `publishAsync()` (notificações assíncronas)

```promql
histogram_quantile(0.95, sum by(le) (rate(courier_publish_async_duration_seconds_bucket[$__rate_interval])))
```

### Latência média de `send()`

```promql
rate(courier_send_duration_seconds_sum[$__rate_interval])
  /
rate(courier_send_duration_seconds_count[$__rate_interval])
```

---

## 🚨 Erros e Confiabilidade

### Taxa total de falhas em requests (erros de handler + falhas de validação)

```promql
  sum(rate(courier_handler_errors_total[$__rate_interval]))
+ sum(rate(courier_validation_failures_total[$__rate_interval]))
```

> **Nota:** A partir da v2.0.5, `courier.handler.errors` e
> `courier.validation.failures` são mutuamente exclusivos — um request com
> falha incrementa um **ou** outro, nunca ambos. Use a query composta acima
> quando precisar de uma série temporal única de "todas as falhas".

### Taxa de erros em handlers

```promql
sum(rate(courier_handler_errors_total[$__rate_interval]))
```

### Erros por tipo de exceção

```promql
sum by(exception_type) (rate(courier_handler_errors_total[$__rate_interval]))
```

### Erros por tipo de request

```promql
sum by(request_type) (rate(courier_handler_errors_total[$__rate_interval]))
```

### Taxa de falhas de validação

```promql
sum(rate(courier_validation_failures_total[$__rate_interval]))
```

### Falhas de validação por tipo de request

```promql
sum by(request_type) (rate(courier_validation_failures_total[$__rate_interval]))
```

### Taxa de timeouts

```promql
sum(rate(courier_handler_timeouts_total[$__rate_interval]))
```

### Error ratio (% de sends com erro)

```promql
sum(rate(courier_send_total{outcome="error"}[$__rate_interval]))
  /
sum(rate(courier_send_total[$__rate_interval]))
```

---

## 📦 Registry (Gauges)

### Handlers registrados

```promql
courier_handlers_registered
```

### Notification handlers registrados

```promql
courier_notification_handlers_registered
```

### Pipeline behaviors registrados

```promql
courier_pipeline_behaviors_registered
```

---

## 🔔 Alertas Sugeridos (Alertmanager)

### Error ratio acima de 5% por 5 minutos

```yaml
- alert: CourierHighErrorRatio
  expr: |
    sum(rate(courier_send_total{outcome="error"}[5m]))
      /
    sum(rate(courier_send_total[5m]))
    > 0.05
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Spring Courier error ratio above 5%"
    description: "{{ $value | humanizePercentage }} of sends are failing."
```

### p99 acima de 1 segundo por 5 minutos

```yaml
- alert: CourierHighLatency
  expr: |
    histogram_quantile(0.99,
      sum by(le) (rate(courier_send_duration_seconds_bucket[5m]))
    ) > 1.0
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Spring Courier p99 latency above 1s"
    description: "p99 send latency is {{ $value }}s."
```

### Timeouts detectados

```yaml
- alert: CourierHandlerTimeouts
  expr: sum(rate(courier_handler_timeouts_total[5m])) > 0
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Spring Courier handler timeouts detected"
    description: "{{ $value }} timeouts/s in the last 5 minutes."
```

### Validation failure spike

```yaml
- alert: CourierValidationSpike
  expr: sum(rate(courier_validation_failures_total[5m])) > 10
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High validation failure rate in Spring Courier"
    description: "{{ $value }} validation failures/s."
```

---

## 🏷️ Filtros Comuns

Insira estes seletores de labels em qualquer query acima:

```promql
# Filtrar por aplicação
{application="my-service"}

# Filtrar por instância
{instance="my-service:8080"}

# Apenas commands
{request_category="command"}

# Apenas queries
{request_category="query"}

# Apenas erros
{outcome="error"}

# Apenas um tipo de request
{request_type="CreateProductCommand"}
```

---

## � Alertas no Slack

Os alertas sugeridos acima estão disponíveis como **regras provisionáveis do Grafana** com integração Slack pronta para uso.

Consulte o guia completo: **[SLACK_ALERTING.md](./SLACK_ALERTING.md)**

Arquivos de provisioning:

| Arquivo | Descrição |
|---------|-----------|
| `provisioning/alerting/courier-alert-rules.yml` | 6 regras de alerta (Grafana Unified Alerting) |
| `provisioning/alerting/courier-slack-notifications.yml` | Contact points Slack + notification policies |

---

## 📖 Referências

- [Micrometer Concepts](https://micrometer.io/docs/concepts)
- [Prometheus Query Basics](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana — Prometheus Data Source](https://grafana.com/docs/grafana/latest/datasources/prometheus/)
- [Grafana Unified Alerting](https://grafana.com/docs/grafana/latest/alerting/)
- [Slack Incoming Webhooks](https://api.slack.com/messaging/webhooks)
