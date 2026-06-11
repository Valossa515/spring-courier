package io.github.valossa515.spring_courier.core.pipelines;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestKeySupportTest {

    record WithToString(String id) {
    }

    static class CustomToString {
        @Override
        public String toString() {
            return "custom";
        }
    }

    static class PlainClass {
    }

    @Test
    void recordsHaveCustomToString() {
        assertTrue(RequestKeySupport.hasCustomToString(WithToString.class));
    }

    @Test
    void explicitOverrideIsDetected() {
        assertTrue(RequestKeySupport.hasCustomToString(CustomToString.class));
    }

    @Test
    void plainClassUsesObjectToString() {
        assertFalse(RequestKeySupport.hasCustomToString(PlainClass.class));
    }

    @Test
    void utilityConstructorIsPrivateButInvocable() throws Exception {
        Constructor<RequestKeySupport> constructor =
                RequestKeySupport.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
