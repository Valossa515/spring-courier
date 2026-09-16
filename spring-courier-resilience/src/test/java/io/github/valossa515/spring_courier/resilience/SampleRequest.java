package io.github.valossa515.spring_courier.resilience;

import io.github.valossa515.spring_courier.core.interfaces.IRequest;

/** Stand-in request used across the resilience tests. */
public record SampleRequest(String id) implements IRequest<String> {
}
