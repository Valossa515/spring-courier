package io.github.valossa515.spring_courier.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

class RedisIdempotencyStoreTest {

    private static final String PREFIX = "courier:idem:";

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, Object> valueOps = mock(ValueOperations.class);

    private RedisIdempotencyStore store;

    @BeforeEach
    void setUp() {
        when(template.opsForValue()).thenReturn(valueOps);
        store = new RedisIdempotencyStore(template, PREFIX);
    }

    @Test
    void getReadsPrefixedKey() {
        when(valueOps.get(PREFIX + "k")).thenReturn("recorded");

        assertThat(store.get("k")).contains("recorded");
    }

    @Test
    void getReturnsEmptyWhenAbsent() {
        when(valueOps.get(PREFIX + "k")).thenReturn(null);

        assertThat(store.get("k")).isEmpty();
    }

    @Test
    void putWithTtlSetsExpiringKey() {
        store.put("k", "v", Duration.ofSeconds(30));

        verify(valueOps).set(PREFIX + "k", "v", Duration.ofSeconds(30));
    }

    @Test
    void putWithoutTtlSetsKeyWithoutExpiry() {
        store.put("k", "v", Duration.ZERO);

        verify(valueOps).set(PREFIX + "k", "v");
    }

    @Test
    void removeDeletesPrefixedKey() {
        store.remove("k");

        verify(template).delete(PREFIX + "k");
    }

    @Test
    @SuppressWarnings("unchecked")
    void clearScansAndDeletesEveryKey() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(PREFIX + "a");
        when(template.scan(any(ScanOptions.class))).thenReturn(cursor);

        store.clear();

        verify(template).delete(List.of(PREFIX + "a"));
    }

    @Test
    void sizeIsNotReported() {
        assertThat(store.size()).isEqualTo(-1);
    }
}
