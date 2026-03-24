package io.github.valossa515.spring_courier.core.support;

import io.github.valossa515.spring_courier.core.interfaces.ResponseEntityConverter;
import org.springframework.http.ResponseEntity;

/**
 * Default {@link ResponseEntityConverter} that delegates to
 * {@link Response#toEntity()}.  Registered as a Spring bean by the
 * autoconfiguration and replaceable via
 * {@link org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean}.
 */
public class DefaultResponseEntityConverter implements ResponseEntityConverter {

    @Override
    public ResponseEntity<?> convert(Response<?> response) {
        if (response == null) {
            return ResponseEntity.internalServerError().build();
        }
        return response.toEntity();
    }
}
