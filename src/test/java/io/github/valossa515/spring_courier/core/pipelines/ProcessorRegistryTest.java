package io.github.valossa515.spring_courier.core.pipelines;

import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.IRequestExceptionHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessorRegistryTest {

    @Test
    void registersPreProcessor() {
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerPreProcessor(
                (PreProcessor<TestCmd>) request -> { });

        assertEquals(1, registry.getPreProcessors().size());
    }

    @Test
    void registersPostProcessor() {
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerPostProcessor(
                (PostProcessor<TestCmd, Object>)
                        (request, response) -> { });

        assertEquals(1, registry.getPostProcessors().size());
    }

    @Test
    void registersExceptionHandler() {
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerExceptionHandler(
                (IRequestExceptionHandler<TestCmd, Object>)
                        (request, ex) -> null);

        assertEquals(1, registry.getExceptionHandlers().size());
    }

    @Test
    void freezePreventsRegistration() {
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.freeze();
        assertTrue(registry.isFrozen());

        assertThrows(IllegalStateException.class,
                () -> registry.registerPreProcessor(
                        (PreProcessor<TestCmd>) r -> { }));
        assertThrows(IllegalStateException.class,
                () -> registry.registerPostProcessor(
                        (PostProcessor<TestCmd, Object>)
                                (r, s) -> { }));
        assertThrows(IllegalStateException.class,
                () -> registry.registerExceptionHandler(
                        (IRequestExceptionHandler<TestCmd, Object>)
                                (r, e) -> null));
    }

    @Test
    void rejectsNullRegistrations() {
        ProcessorRegistry registry = new ProcessorRegistry();
        assertThrows(NullPointerException.class,
                () -> registry.registerPreProcessor(null));
        assertThrows(NullPointerException.class,
                () -> registry.registerPostProcessor(null));
        assertThrows(NullPointerException.class,
                () -> registry.registerExceptionHandler(null));
    }

    @Test
    void initiallyNotFrozen() {
        ProcessorRegistry registry = new ProcessorRegistry();
        assertFalse(registry.isFrozen());
    }

    static class TestCmd implements ICommand<String> {
    }
}
