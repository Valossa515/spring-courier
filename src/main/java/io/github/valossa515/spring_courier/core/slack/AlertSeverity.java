package io.github.valossa515.spring_courier.core.slack;

/**
 * Severity levels for Slack alerts, following standard operational conventions.
 */
public enum AlertSeverity {

    WARNING("warning", "\u26a0\ufe0f"),
    CRITICAL("critical", "\ud83d\udea8");

    private final String label;
    private final String emoji;

    AlertSeverity(String label, String emoji) {
        this.label = label;
        this.emoji = emoji;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }
}
