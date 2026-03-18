> 🌐 **Language / Idioma:** 🇧🇷 **Português** (atual) | 🇺🇸 [English](SLACK_ALERTING.md)

# Slack Alerting — Spring Courier

Guia para configurar alertas do Spring Courier no Slack via Grafana Unified Alerting.

---

## Visão Geral

O Spring Courier exporta métricas via Micrometer que são coletadas pelo Prometheus e visualizadas no Grafana. Esta configuração adiciona **alertas automatizados para o Slack** baseados nessas mesmas métricas.

### Alertas Disponíveis

| Alerta | Condição | Severidade | `for` |
|--------|----------|------------|-------|
| **High Error Ratio** | Error ratio > 5% | `warning` | 5 min |
| **High p99 Latency** | p99 send > 1s | `warning` | 5 min |
| **Handler Timeouts** | Timeouts detectados | `critical` | 5 min |
| **Validation Spike** | Falhas de validação > 10/s | `warning` | 5 min |
| **Throughput Drop** | Queda de throughput > 50% | `critical` | 10 min |
| **High Error by Exception** | Erros por exceção > 1/s | `warning` | 5 min |

### Arquitetura

```
Spring Boot App               Prometheus           Grafana              Slack
┌─────────────┐   scrape    ┌───────────┐  query  ┌──────────┐  POST  ┌───────────┐
│  Micrometer │ ──────────► │ Prometheus │ ◄────── │  Alert   │ ─────► │  #channel │
│  Metrics    │             │  TSDB      │         │  Rules   │        │  webhook  │
└─────────────┘             └───────────┘         └──────────┘        └───────────┘
                                                   │ routing  │
                                                   ▼          ▼
                                              critical    warning
                                              @here        normal
```

---

## Pré-requisitos

- **Grafana** >= 10.0 com Unified Alerting habilitado
- **Prometheus** como datasource configurado no Grafana
- **Slack Incoming Webhook** ou **Slack Bot Token**
- Métricas do Spring Courier sendo coletadas pelo Prometheus

---

## Configuração Rápida

### 1. Criar Slack Incoming Webhook

1. Acesse [Slack API — Incoming Webhooks](https://api.slack.com/messaging/webhooks)
2. Crie um novo App ou use um existente
3. Ative **Incoming Webhooks**
4. Adicione um webhook para o canal desejado (ex: `#courier-alerts`)
5. Copie a URL do webhook (formato: `https://hooks.slack.com/services/T.../B.../xxx`)

> **Segurança:** Nunca commite a webhook URL diretamente nos arquivos de configuração. Use variáveis de ambiente.

### 2. Configurar Variáveis de Ambiente

Defina as seguintes variáveis de ambiente antes de iniciar o Grafana:

```bash
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/T.../B.../xxx"
export SLACK_CHANNEL="#courier-alerts"
```

### 3. Copiar Arquivos de Provisioning

Copie os arquivos de provisioning para o diretório do Grafana:

```bash
# Diretório de provisioning do Grafana (ajuste conforme instalação)
GRAFANA_PROVISIONING="/etc/grafana/provisioning"

# Copiar regras de alerta
cp docs/grafana/provisioning/alerting/courier-alert-rules.yml \
   $GRAFANA_PROVISIONING/alerting/

# Copiar contact points e notification policies
cp docs/grafana/provisioning/alerting/courier-slack-notifications.yml \
   $GRAFANA_PROVISIONING/alerting/
```

### 4. Ajustar o Datasource UID

Nos arquivos de regras, substitua `${DS_PROMETHEUS}` pelo UID real do seu datasource Prometheus no Grafana:

```bash
# Descubra o UID do datasource
curl -s http://admin:admin@localhost:3000/api/datasources | jq '.[].uid'

# Substitua nos arquivos (exemplo com sed)
sed -i 's/\${DS_PROMETHEUS}/seu-datasource-uid/g' \
   $GRAFANA_PROVISIONING/alerting/courier-alert-rules.yml
```

### 5. Reiniciar o Grafana

```bash
sudo systemctl restart grafana-server
# ou
docker restart grafana
```

---

## Configuração via Docker Compose

Se você utiliza Docker Compose, monte os arquivos de provisioning diretamente:

```yaml
services:
  grafana:
    image: grafana/grafana:10.4.0
    ports:
      - "3000:3000"
    environment:
      - SLACK_WEBHOOK_URL=${SLACK_WEBHOOK_URL}
      - SLACK_CHANNEL=${SLACK_CHANNEL}
      - GF_UNIFIED_ALERTING_ENABLED=true
    volumes:
      # Dashboard existente
      - ./docs/grafana/courier-dashboard.json:/var/lib/grafana/dashboards/courier-dashboard.json
      # Provisioning de alertas
      - ./docs/grafana/provisioning/alerting/courier-alert-rules.yml:/etc/grafana/provisioning/alerting/courier-alert-rules.yml
      - ./docs/grafana/provisioning/alerting/courier-slack-notifications.yml:/etc/grafana/provisioning/alerting/courier-slack-notifications.yml
      # Provisioning de dashboards
      - ./docs/grafana/provisioning/dashboards/:/etc/grafana/provisioning/dashboards/
```

---

## Configuração Manual (via UI do Grafana)

Se preferir configurar via interface web:

### Contact Point

1. Vá em **Alerting → Contact points → Add contact point**
2. Nome: `courier-slack-critical`
3. Tipo: **Slack**
4. Webhook URL: cole a URL do webhook
5. Recipient: `#courier-alerts`
6. Title template:
   ```
   {{ .Status | toUpper }} | {{ .CommonLabels.alert_type }} | {{ .CommonLabels.severity | toUpper }}
   ```
7. Text body template:
   ```
   *Alert:* {{ .CommonLabels.alertname }}
   *Severity:* `{{ .CommonLabels.severity }}`
   *Component:* `{{ .CommonLabels.component }}`

   {{ range .Alerts }}
   :rotating_light: *{{ .Annotations.summary }}*
   {{ .Annotations.description }}
   {{ end }}

   :chart_with_upwards_trend: <{{ .ExternalURL }}|Ver no Grafana>
   ```
8. Clique em **Test** para validar a integração
9. Salve

### Notification Policy

1. Vá em **Alerting → Notification policies**
2. Edite a política default ou crie sub-rotas:
   - **Matcher:** `component = spring-courier` e `severity = critical`
   - **Contact point:** `courier-slack-critical`
   - **Group wait:** 10s | **Group interval:** 1m | **Repeat interval:** 1h

---

## Personalização dos Alertas

### Ajustar Thresholds

Edite o arquivo `courier-alert-rules.yml` e ajuste os valores conforme sua realidade:

| Parâmetro | Default | Onde ajustar |
|-----------|---------|--------------|
| Error ratio threshold | 5% (0.05) | `courier-high-error-ratio` → params |
| p99 latency threshold | 1.0s | `courier-high-latency` → params |
| Validation rate threshold | 10/s | `courier-validation-spike` → params |
| Throughput drop threshold | 50% (0.5) | `courier-throughput-drop` → params |
| Evaluation `for` duration | 5m–10m | Cada regra → campo `for` |

### Adicionar Canais Separados por Severidade

Crie múltiplos contact points para rotear alertas para canais diferentes:

```yaml
# Em courier-slack-notifications.yml
contactPoints:
  - orgId: 1
    name: courier-slack-ops
    receivers:
      - uid: courier-slack-ops
        type: slack
        settings:
          url: "${SLACK_WEBHOOK_URL_OPS}"
          recipient: "#ops-critical"
          mention_channel: "channel"  # @channel para urgente

  - orgId: 1
    name: courier-slack-dev
    receivers:
      - uid: courier-slack-dev
        type: slack
        settings:
          url: "${SLACK_WEBHOOK_URL_DEV}"
          recipient: "#dev-warnings"
```

### Mute Timings (Janelas de Manutenção)

Descomente a seção `muteTimes` em `courier-slack-notifications.yml` para silenciar alertas durante janelas de manutenção:

```yaml
muteTimes:
  - orgId: 1
    name: courier-maintenance-window
    time_intervals:
      - times:
          - start_time: "02:00"
            end_time: "04:00"
        weekdays:
          - "sunday"
        location: "America/Sao_Paulo"
```

---

## Exemplos de Notificação no Slack

### Alerta Critical (com @here)

```
🚨 FIRING | timeout | CRITICAL
──────────────────────
Alert: Courier: Handler Timeouts Detected
Severity: critical
Component: spring-courier

🔺 Handler timeouts detectados no Spring Courier
0.5 timeouts/s nos últimos 5 minutos.
Verifique handlers assíncronos e o valor de spring.courier.async-timeout-ms.

📈 Ver no Grafana
```

### Alerta Warning

```
⚠️ FIRING | error-ratio | WARNING
──────────────────────
Alert: Courier: High Error Ratio
Severity: warning
Component: spring-courier

⚠️ Spring Courier error ratio acima de 5%
8.3% dos sends estão falhando nos últimos 5 minutos.

📈 Ver no Grafana
```

### Alerta Resolvido

```
✅ RESOLVED | error-ratio | WARNING
──────────────────────
Alert: Courier: High Error Ratio
Severity: warning
Component: spring-courier

✅ Spring Courier error ratio voltou ao normal.

📈 Ver no Grafana
```

---

## Troubleshooting

### Alertas não disparam

1. Verifique se o Prometheus está coletando as métricas:
   ```promql
   courier_send_total
   ```
2. Confirme que o datasource UID está correto nas regras
3. Verifique os logs do Grafana:
   ```bash
   docker logs grafana 2>&1 | grep -i alert
   ```

### Slack não recebe notificações

1. Teste o webhook manualmente:
   ```bash
   curl -X POST -H 'Content-type: application/json' \
     --data '{"text":"Teste de alerta Spring Courier"}' \
     "$SLACK_WEBHOOK_URL"
   ```
2. Verifique se o contact point está configurado corretamente:
   **Alerting → Contact points → Test**
3. Confirme que a notification policy associa o label correto ao contact point

### Alertas em "No Data"

Isso ocorre quando o Prometheus não tem dados para a query. Causas comuns:
- Aplicação ainda não enviou requests (counters são criados no primeiro uso)
- Scrape interval muito alto
- Labels de filtro incorretos

---

## Referências

- [Grafana Unified Alerting](https://grafana.com/docs/grafana/latest/alerting/)
- [Grafana Slack Contact Point](https://grafana.com/docs/grafana/latest/alerting/configure-notifications/manage-contact-points/integrations/configure-slack/)
- [Grafana Provisioning — Alerting](https://grafana.com/docs/grafana/latest/alerting/set-up/provision-alerting-resources/file-provisioning/)
- [Slack Incoming Webhooks](https://api.slack.com/messaging/webhooks)
- [PROMQL_REFERENCE.md](./PROMQL_REFERENCE.md) — Consultas PromQL do Spring Courier
