package io.github.valossa515.spring_courier.core.interfaces;

import io.github.valossa515.spring_courier.core.support.Response;
import org.springframework.http.ResponseEntity;

/**
 * Strategy interface for converting a {@link Response} into a Spring
 * {@link ResponseEntity}. A default implementation is auto-configured
 * by Spring Courier, but applications may provide their own bean to
 * customise response conversion (e.g. file downloads, custom headers,
 * or content negotiation).
 *
 * <p>The default bean is registered with
 * {@link org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean},
 * so declaring a custom {@code ResponseEntityConverter} bean in the
 * application context is enough to replace it.
 *
 * <p>Example usage:
 * <pre>{@code
 * @RestController
 * public class ProductController {
 *     @Autowired Courier courier;
 *     @Autowired ResponseEntityConverter converter;
 *
 *     @PostMapping("/products")
 *     public ResponseEntity<?> create(@RequestBody CreateProductCommand cmd) {
 *         return converter.convert(courier.send(cmd));
 *     }
 * }
 * }</pre>
 *
 * @see Response#toEntity()
 */
@FunctionalInterface
public interface ResponseEntityConverter {

    /**
     * Converts the given {@link Response} into a {@link ResponseEntity}.
     *
     * @param response the response produced by the Courier pipeline
     * @param <T>      the response data type
     * @return a {@link ResponseEntity} suitable for returning from a controller
     */
    <T> ResponseEntity<?> convert(Response<T> response);
}
