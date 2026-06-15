package io.github.valossa515.spring_courier.outbox;

import io.github.valossa515.spring_courier.core.interfaces.INotification;

/** Simple notification used across the outbox tests. */
public record SampleNotification(String id, int value) implements INotification {
}
