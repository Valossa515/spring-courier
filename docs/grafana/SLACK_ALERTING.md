# Slack Alerting — Spring Courier

Guide for setting up Spring Courier alerts on Slack via Grafana Unified Alerting.

---

## Overview

Spring Courier exports metrics via Micrometer that are collected by Prometheus and visualized in Grafana. This configuration adds **automated Slack alerts** based on those same metrics.

### Available Alerts

| Alert | Condition | Severity | `for` |
|-------|-----------|----------|-------|
| **High Error Ratio** | Error ratio > 5% | `warning` | 5 min |
| **High p99 Latency** | p99 send > 1s | `warning` | 5 min |
| **Handler Timeouts** | Timeouts detected | `critical` | 5 min |
| **Validation Spike** | Validation failures > 10/s | `warning` | 5 min |
| **Throughput Drop** | Throughput drop > 50% | `critical` | 10 min |
| **High Error by Exception** | Errors per exception > 1/s | `warning` | 5 min |

### Architecture

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

## Prerequisites

- **Grafana** >= 10.0 with Unified Alerting enabled
- **Prometheus** configured as a datasource in Grafana
- **Slack Incoming Webhook** or **Slack Bot Token**
- Spring Courier metrics being collected by Prometheus

---

## Quick Setup

### 1. Create a Slack Incoming Webhook

1. Go to [Slack API — Incoming Webhooks](https://api.slack.com/messaging/webhooks)
2. Create a new App or use an existing one
3. Enable **Incoming Webhooks**
4. Add a webhook for the desired channel (e.g., `#courier-alerts`)
5. Copy the webhook URL (format: `https://hooks.slack.com/services/T.../B.../xxx`)

> **Security:** Never commit the webhook URL directly in configuration files. Use environment variables.

### 2. Set Environment Variables

Set the following environment variables before starting Grafana:

```bash
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/T.../B.../xxx"
export SLACK_CHANNEL="#courier-alerts"
```

### 3. Copy Provisioning Files

Copy the provisioning files to the Grafana directory:

```bash
# Grafana provisioning directory (adjust according to your installation)
GRAFANA_PROVISIONING="/etc/grafana/provisioning"

# Copy alert rules
cp docs/grafana/provisioning/alerting/courier-alert-rules.yml \
   $GRAFANA_PROVISIONING/alerting/

# Copy contact points and notification policies
cp docs/grafana/provisioning/alerting/courier-slack-notifications.yml \
   $GRAFANA_PROVISIONING/alerting/
```

### 4. Adjust the Datasource UID

In the rule files, replace `${DS_PROMETHEUS}` with the actual UID of your Prometheus datasource in Grafana:

```bash
# Find the datasource UID
curl -s http://admin:admin@localhost:3000/api/datasources | jq '.[].uid'

# Replace in files (example with sed)
sed -i 's/\${DS_PROMETHEUS}/your-datasource-uid/g' \
   $GRAFANA_PROVISIONING/alerting/courier-alert-rules.yml
```

### 5. Restart Grafana

```bash
sudo systemctl restart grafana-server
# or
docker restart grafana
```

---

## Docker Compose Setup

If you use Docker Compose, mount the provisioning files directly:

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
      # Existing dashboard
      - ./docs/grafana/courier-dashboard.json:/var/lib/grafana/dashboards/courier-dashboard.json
      # Alert provisioning
      - ./docs/grafana/provisioning/alerting/courier-alert-rules.yml:/etc/grafana/provisioning/alerting/courier-alert-rules.yml
      - ./docs/grafana/provisioning/alerting/courier-slack-notifications.yml:/etc/grafana/provisioning/alerting/courier-slack-notifications.yml
      # Dashboard provisioning
      - ./docs/grafana/provisioning/dashboards/:/etc/grafana/provisioning/dashboards/
```

---

## Manual Setup (via Grafana UI)

If you prefer to configure via the web interface:

### Contact Point

1. Go to **Alerting → Contact points → Add contact point**
2. Name: `courier-slack-critical`
3. Type: **Slack**
4. Webhook URL: paste the webhook URL
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

   :chart_with_upwards_trend: <{{ .ExternalURL }}|View in Grafana>
   ```
8. Click **Test** to validate the integration
9. Save

### Notification Policy

1. Go to **Alerting → Notification policies**
2. Edit the default policy or create sub-routes:
   - **Matcher:** `component = spring-courier` and `severity = critical`
   - **Contact point:** `courier-slack-critical`
   - **Group wait:** 10s | **Group interval:** 1m | **Repeat interval:** 1h

---

## Customizing Alerts

### Adjusting Thresholds

Edit the `courier-alert-rules.yml` file and adjust values according to your needs:

| Parameter | Default | Where to adjust |
|-----------|---------|-----------------|
| Error ratio threshold | 5% (0.05) | `courier-high-error-ratio` → params |
| p99 latency threshold | 1.0s | `courier-high-latency` → params |
| Validation rate threshold | 10/s | `courier-validation-spike` → params |
| Throughput drop threshold | 50% (0.5) | `courier-throughput-drop` → params |
| Evaluation `for` duration | 5m–10m | Each rule → `for` field |

### Adding Separate Channels by Severity

Create multiple contact points to route alerts to different channels:

```yaml
# In courier-slack-notifications.yml
contactPoints:
  - orgId: 1
    name: courier-slack-ops
    receivers:
      - uid: courier-slack-ops
        type: slack
        settings:
          url: "${SLACK_WEBHOOK_URL_OPS}"
          recipient: "#ops-critical"
          mention_channel: "channel"  # @channel for urgent

  - orgId: 1
    name: courier-slack-dev
    receivers:
      - uid: courier-slack-dev
        type: slack
        settings:
          url: "${SLACK_WEBHOOK_URL_DEV}"
          recipient: "#dev-warnings"
```

### Mute Timings (Maintenance Windows)

Uncomment the `muteTimes` section in `courier-slack-notifications.yml` to silence alerts during maintenance windows:

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

## Slack Notification Examples

### Critical Alert (with @here)

```
🚨 FIRING | timeout | CRITICAL
──────────────────────
Alert: Courier: Handler Timeouts Detected
Severity: critical
Component: spring-courier

🔺 Handler timeouts detected in Spring Courier
0.5 timeouts/s in the last 5 minutes.
Check async handlers and the spring.courier.async-timeout-ms value.

📈 View in Grafana
```

### Warning Alert

```
⚠️ FIRING | error-ratio | WARNING
──────────────────────
Alert: Courier: High Error Ratio
Severity: warning
Component: spring-courier

⚠️ Spring Courier error ratio above 5%
8.3% of sends are failing in the last 5 minutes.

📈 View in Grafana
```

### Resolved Alert

```
✅ RESOLVED | error-ratio | WARNING
──────────────────────
Alert: Courier: High Error Ratio
Severity: warning
Component: spring-courier

✅ Spring Courier error ratio is back to normal.

📈 View in Grafana
```

---

## Troubleshooting

### Alerts not firing

1. Check that Prometheus is collecting the metrics:
   ```promql
   courier_send_total
   ```
2. Confirm the datasource UID is correct in the rules
3. Check the Grafana logs:
   ```bash
   docker logs grafana 2>&1 | grep -i alert
   ```

### Slack not receiving notifications

1. Test the webhook manually:
   ```bash
   curl -X POST -H 'Content-type: application/json' \
     --data '{"text":"Spring Courier alert test"}' \
     "$SLACK_WEBHOOK_URL"
   ```
2. Check that the contact point is configured correctly:
   **Alerting → Contact points → Test**
3. Confirm that the notification policy associates the correct label with the contact point

### Alerts showing "No Data"

This occurs when Prometheus has no data for the query. Common causes:
- Application hasn't sent any requests yet (counters are created on first use)
- Scrape interval too high
- Incorrect filter labels

---

## References

- [Grafana Unified Alerting](https://grafana.com/docs/grafana/latest/alerting/)
- [Grafana Slack Contact Point](https://grafana.com/docs/grafana/latest/alerting/configure-notifications/manage-contact-points/integrations/configure-slack/)
- [Grafana Provisioning — Alerting](https://grafana.com/docs/grafana/latest/alerting/set-up/provision-alerting-resources/file-provisioning/)
- [Slack Incoming Webhooks](https://api.slack.com/messaging/webhooks)
- [PROMQL_REFERENCE.md](./PROMQL_REFERENCE.md) — Spring Courier PromQL queries
