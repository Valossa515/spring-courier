package io.github.valossa515.spring_courier.config;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.support.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierRuntimeHintsTest {

    private RuntimeHints hints;

    @BeforeEach
    void setUp() {
        hints = new RuntimeHints();
        new CourierRuntimeHints()
                .registerHints(hints, getClass().getClassLoader());
    }

    @Test
    void courierClassIsRegistered() {
        assertTrue(RuntimeHintsPredicates.reflection()
                .onType(Courier.class).test(hints));
    }

    @Test
    void responseClassIsRegistered() {
        assertTrue(RuntimeHintsPredicates.reflection()
                .onType(Response.class).test(hints));
    }

    @Test
    void handlerInterfacesAreRegistered() {
        assertTrue(RuntimeHintsPredicates.reflection()
                .onType(CommandHandler.class).test(hints));
    }

    @Test
    void requestInterfaceIsRegistered() {
        assertTrue(RuntimeHintsPredicates.reflection()
                .onType(IRequest.class).test(hints));
    }
}
