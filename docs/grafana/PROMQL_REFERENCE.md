> 🌐 **Language / Idioma:** 🇧🇷 [Português](PROMQL_REFERENCE.pt-BR.md) | 🇺🇸 **English** (current)

# PromQL Reference — Spring Courier Metrics

Ready-to-use PromQL queries for monitoring Spring Courier with Prometheus and Grafana.

> **Note:** Micrometer automatically converts metric names from dot-notation (`courier.send.duration`) to Prometheus format with underscores (`courier_send_duration_seconds`).

---

## 📬 Throughput

### Request (send) rate per second

```promql
sum(rate(courier_send_total[$__rate_interval]))
```

### Request rate by type

```promql
sum by(request_type) (rate(courier_send_total[$__rate_interval]))
```

### Request rate by category (command vs query)

```promql
sum by(request_category) (rate(courier_send_duration_seconds_count[$__rate_interval]))
```

### Published notifications per second

```promql
sum(rate(courier_publish_total[$__rate_interval]))
```

### Notification rate by type

```promql
sum by(notification_type) (rate(courier_publish_total[$__rate_interval]))
```

---

## ⏱️ Latency

### p50 / p95 / p99 for `send()`

```promql
# p50
histogram_quantile(0.50, sum by(le) (rate(courier_send_duration_seconds_bucket[$__rate_interval])))

# p95
histogram_quantile(0.95, sum by(le) (rate(courier_send_duration_seconds_bucket[$__rate_interval])))

# p99
histogram_quantile(0.99, sum by(le) (rate(courier_send_duration_seconds_bucket[$__rate_interval])))
```

### p95 for `send()` by request type

```promql
histogram_quantile(0.95, sum by(le, request_type) (rate(courier_send_duration_seconds_bucket[$__rate_interval])))
```

### p95 for `publish()` (synchronous notifications)

```promql
histogram_quantile(0.95, sum by(le) (rate(courier_publish_duration_seconds_bucket[$__rate_interval])))
```

### p95 for `publishAsync()` (asynchronous notifications)

```promql
histogram_quantile(0.95, sum by(le) (rate(courier_publish_async_duration_seconds_bucket[$__rate_interval])))
```

### Average `send()` latency

```promql
rate(courier_send_duration_seconds_sum[$__rate_interval])
  /
rate(courier_send_duration_seconds_count[$__rate_interval])
```

---

## 🚨 Errors and Reliability

### Total request failure rate (handler errors + validation failures)

```promql
  sum(rate(courier_handler_errors_total[$__rate_interval]))
+ sum(rate(courier_validation_failures_total[$__rate_interval]))
```

> **Note:** Since v2.0.5, `courier.handler.errors` and
> `courier.validation.failures` are mutually exclusive — a failed request
> increments one **or** the other, never both. Use the composite query above
> when you need a single "all failures" time series.

### Handler error rate

```promql
sum(rate(courier_handler_errors_total[$__rate_interval]))
```

### Errors by exception type

```promql
sum by(exception_type) (rate(courier_handler_errors_total[$__rate_interval]))
```

### Errors by request type

```promql
sum by(request_type) (rate(courier_handler_errors_total[$__rate_interval]))
```

### Validation failure rate

```promql
sum(rate(courier_validation_failures_total[$__rate_interval]))
```

### Validation failures by request type

```promql
sum by(request_type) (rate(courier_validation_failures_total[$__rate_interval]))
```

### Timeout rate

```promql
sum(rate(courier_handler_timeouts_total[$__rate_interval]))
```

### Error ratio (% of sends with error)

```promql
sum(rate(courier_send_total{outcome="error"}[$__rate_interval]))
  /
sum(rate(courier_send_total[$__rate_interval]))
```

---

## 📦 Registry (Gauges)

### Registered handlers

```promql
courier_handlers_registered
```

### Registered notification handlers

```promql
courier_notification_handlers_registered
```

### Registered pipeline behaviors

```promql
courier_pipeline_behaviors_registered
```

---

## � Cache

### Cache hit rate

```promql
sum(rate(courier_cache_hits_total[$__rate_interval]))
```

### Cache miss rate

```promql
sum(rate(courier_cache_misses_total[$__rate_interval]))
```

### Cache hit ratio

```promql
sum(rate(courier_cache_hits_total[$__rate_interval]))
  /
(sum(rate(courier_cache_hits_total[$__rate_interval])) + sum(rate(courier_cache_misses_total[$__rate_interval])))
```

---

## 🔄 Retry

### Retry attempt rate

```promql
sum(rate(courier_retry_attempts_total[$__rate_interval]))
```

### Retry exhaustion rate (all attempts failed)

```promql
sum(rate(courier_retry_exhausted_total[$__rate_interval]))
```

---

## 📦 Batch & Async

### p95 for `sendAsync()`

```promql
histogram_quantile(0.95, sum by(le) (rate(courier_send_async_duration_seconds_bucket[$__rate_interval])))
```

### p95 for batch `sendAll()` / `sendAllAsync()`

```promql
histogram_quantile(0.95, sum by(le) (rate(courier_batch_send_duration_seconds_bucket[$__rate_interval])))
```

### Average batch size

```promql
rate(courier_batch_send_size_sum[$__rate_interval])
  /
rate(courier_batch_send_size_count[$__rate_interval])
```

### In-flight requests

```promql
sum(courier_requests_in_flight_active_count)
```

---

## �🔔 Suggested Alerts (Alertmanager)

### Error ratio above 5% for 5 minutes

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

### p99 above 1 second for 5 minutes

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

### Timeouts detected

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

## 🏷️ Common Filters

Insert these label selectors into any query above:

```promql
# Filter by application
{application="my-service"}

# Filter by instance
{instance="my-service:8080"}

# Commands only
{request_category="command"}

# Queries only
{request_category="query"}

# Errors only
{outcome="error"}

# Specific request type only
{request_type="CreateProductCommand"}
```

---

## 🔔 Slack Alerts

The suggested alerts above are available as **provisionable Grafana rules** with ready-to-use Slack integration.

See the full guide: **[SLACK_ALERTING.md](./SLACK_ALERTING.md)**

Provisioning files:

| File | Description |
|------|-------------|
| `provisioning/alerting/courier-alert-rules.yml` | 6 alert rules (Grafana Unified Alerting) |
| `provisioning/alerting/courier-slack-notifications.yml` | Slack contact points + notification policies |

---

## 📖 References

- [Micrometer Concepts](https://micrometer.io/docs/concepts)
- [Prometheus Query Basics](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana — Prometheus Data Source](https://grafana.com/docs/grafana/latest/datasources/prometheus/)
- [Grafana Unified Alerting](https://grafana.com/docs/grafana/latest/alerting/)
- [Slack Incoming Webhooks](https://api.slack.com/messaging/webhooks)
