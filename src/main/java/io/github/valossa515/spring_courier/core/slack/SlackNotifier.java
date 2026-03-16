package io.github.valossa515.spring_courier.core.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Sends alert messages to a Slack Incoming Webhook.
 *
 * <p>Uses {@link java.net.http.HttpClient} (Java 11+) so no extra
 * dependencies are required. Messages are sent asynchronously to avoid
 * blocking the alert evaluation loop.
 */
public class SlackNotifier {

    private static final Logger logger = LoggerFactory.getLogger(SlackNotifier.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int HTTP_OK = 200;

    private final URI webhookUri;
    private final String channel;
    private final HttpClient httpClient;

    public SlackNotifier(String webhookUrl, String channel) {
        this.webhookUri = URI.create(webhookUrl);
        this.channel = channel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    SlackNotifier(String webhookUrl, String channel, HttpClient httpClient) {
        this.webhookUri = URI.create(webhookUrl);
        this.channel = channel;
        this.httpClient = httpClient;
    }

    /**
     * Sends a firing alert to Slack asynchronously.
     */
    public CompletableFuture<Void> sendFiring(String alertName,
                                              AlertSeverity severity,
                                              String summary,
                                              String detail,
                                              String appName) {
        String emoji = severity.getEmoji();
        String status = "FIRING";
        String mention = severity == AlertSeverity.CRITICAL ? " <!here>" : "";

        String text = emoji + " *" + status + "* | "
                + alertName + " | `" + severity.getLabel().toUpperCase() + "`"
                + mention + "\n"
                + "───────────────────────\n"
                + "*Alert:* " + summary + "\n"
                + "*Severity:* `" + severity.getLabel() + "`\n"
                + "*App:* `" + appName + "`\n\n"
                + detail;

        return send(text);
    }

    /**
     * Sends a resolved alert to Slack asynchronously.
     */
    public CompletableFuture<Void> sendResolved(String alertName,
                                                String summary,
                                                String appName) {
        String text = "\u2705 *RESOLVED* | " + alertName + "\n"
                + "───────────────────────\n"
                + "*Alert:* " + summary + "\n"
                + "*App:* `" + appName + "`\n\n"
                + "Condition returned to normal.";

        return send(text);
    }

    private CompletableFuture<Void> send(String text) {
        String payload = buildPayload(text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(webhookUri)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != HTTP_OK) {
                        logger.warn("Slack webhook returned status {}: {}",
                                response.statusCode(), response.body());
                    } else {
                        logger.debug("Slack alert sent successfully");
                    }
                })
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof IOException) {
                        logger.error("Failed to send Slack alert (I/O): {}",
                                ex.getCause().getMessage());
                    } else {
                        logger.error("Failed to send Slack alert: {}",
                                ex.getMessage());
                    }
                    return null;
                });
    }

    private String buildPayload(String text) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"text\":").append(escapeJson(text));
        if (channel != null && !channel.isBlank()) {
            sb.append(",\"channel\":").append(escapeJson(channel));
        }
        sb.append('}');
        return sb.toString();
    }

    static String escapeJson(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
