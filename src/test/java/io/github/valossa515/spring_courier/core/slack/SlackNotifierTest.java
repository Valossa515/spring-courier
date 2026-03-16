package io.github.valossa515.spring_courier.core.slack;

import org.junit.jupiter.api.Test;

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
        assertDoesNotThrow(() ->
                new SlackNotifier("https://hooks.slack.com/services/T/B/x",
                        "#test"));
    }

    @Test
    void constructorAcceptsNullChannel() {
        assertDoesNotThrow(() ->
                new SlackNotifier("https://hooks.slack.com/services/T/B/x",
                        null));
    }
}
