package io.github.valossa515.spring_courier.core.slack;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class SlackNotifierTest {

    @Test
    void escapeJsonHandlesQuotes() {
        String result = SlackNotifier.escapeJson("say \"hello\"");
        assertEquals("\"say \\\"hello\\\"\"", result);
    }

    @Test
    void escapeJsonHandlesBackslash() {
        String result = SlackNotifier.escapeJson("path\\to\\file");
        assertEquals("\"path\\\\to\\\\file\"", result);
    }

    @Test
    void escapeJsonHandlesNewlines() {
        String result = SlackNotifier.escapeJson("line1\nline2");
        assertEquals("\"line1\\nline2\"", result);
    }

    @Test
    void escapeJsonHandlesCarriageReturn() {
        String result = SlackNotifier.escapeJson("line1\rline2");
        assertEquals("\"line1\\rline2\"", result);
    }

    @Test
    void escapeJsonHandlesTab() {
        String result = SlackNotifier.escapeJson("col1\tcol2");
        assertEquals("\"col1\\tcol2\"", result);
    }

    @Test
    void escapeJsonHandlesNull() {
        assertEquals("null", SlackNotifier.escapeJson(null));
    }

    @Test
    void escapeJsonHandlesPlainText() {
        String result = SlackNotifier.escapeJson("hello world");
        assertEquals("\"hello world\"", result);
    }

    @Test
    void escapeJsonHandlesEmptyString() {
        String result = SlackNotifier.escapeJson("");
        assertEquals("\"\"", result);
    }

    @Test
    void constructorAcceptsValidUrl() {
        SlackNotifier notifier = new SlackNotifier(
                "https://hooks.slack.com/services/T/B/x", "#test");
        assertDoesNotThrow(() -> {});
        assertTrue(notifier.isConfigured());
    }

    @Test
    void constructorAcceptsNullChannel() {
        SlackNotifier notifier = new SlackNotifier(
                "https://hooks.slack.com/services/T/B/x", null);
        assertDoesNotThrow(() -> {});
        assertTrue(notifier.isConfigured());
    }

    @Test
    void isConfiguredReturnsFalseForBlankUrl() {
        SlackNotifier notifier = new SlackNotifier("", "#test");
        assertFalse(notifier.isConfigured());
    }

    @Test
    void isConfiguredReturnsFalseForNullUrl() {
        SlackNotifier notifier = new SlackNotifier(null, "#test");
        assertFalse(notifier.isConfigured());
    }

    @Test
    void isConfiguredReturnsFalseForUrlWithoutScheme() {
        SlackNotifier notifier = new SlackNotifier(
                "not-a-url", "#test");
        assertFalse(notifier.isConfigured());
    }

    @Test
    void sendFiringReturnsImmediatelyWhenNotConfigured() {
        SlackNotifier notifier = new SlackNotifier("", "#test");
        CompletableFuture<Void> result = notifier.sendFiring(
                "test", AlertSeverity.WARNING,
                "summary", "detail", "app");
        assertNotNull(result);
        assertDoesNotThrow(() -> result.get());
    }

    @Test
    void sendResolvedReturnsImmediatelyWhenNotConfigured() {
        SlackNotifier notifier = new SlackNotifier("", "#test");
        CompletableFuture<Void> result = notifier.sendResolved(
                "test", "summary", "app");
        assertNotNull(result);
        assertDoesNotThrow(() -> result.get());
    }
}
