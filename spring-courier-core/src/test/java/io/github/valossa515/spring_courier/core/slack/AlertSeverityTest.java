package io.github.valossa515.spring_courier.core.slack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertSeverityTest {

    @Test
    void warningHasCorrectLabel() {
        assertEquals("warning", AlertSeverity.WARNING.getLabel());
    }

    @Test
    void criticalHasCorrectLabel() {
        assertEquals("critical", AlertSeverity.CRITICAL.getLabel());
    }

    @Test
    void warningHasEmoji() {
        assertNotNull(AlertSeverity.WARNING.getEmoji());
        assertFalse(AlertSeverity.WARNING.getEmoji().isBlank());
    }

    @Test
    void criticalHasEmoji() {
        assertNotNull(AlertSeverity.CRITICAL.getEmoji());
        assertFalse(AlertSeverity.CRITICAL.getEmoji().isBlank());
    }

    @Test
    void allValuesPresent() {
        assertEquals(2, AlertSeverity.values().length);
    }
}
