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

class RedisCacheStoreTest {

    private static final String PREFIX = "courier:cache:";

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, Object> valueOps = mock(ValueOperations.class);

    private RedisCacheStore store;

    @BeforeEach
    void setUp() {
        when(template.opsForValue()).thenReturn(valueOps);
        store = new RedisCacheStore(template, PREFIX);
    }

    @Test
    void getReadsPrefixedKey() {
        when(valueOps.get(PREFIX + "k")).thenReturn("v");

        assertThat(store.get("k")).contains("v");
    }

    @Test
    void getReturnsEmptyWhenAbsent() {
        when(valueOps.get(PREFIX + "missing")).thenReturn(null);

        assertThat(store.get("missing")).isEmpty();
    }

    @Test
    void putWithTtlSetsExpiringKey() {
        store.put("k", "v", Duration.ofMinutes(5));

        verify(valueOps).set(PREFIX + "k", "v", Duration.ofMinutes(5));
    }

    @Test
    void putWithZeroTtlSetsKeyWithoutExpiry() {
        store.put("k", "v", Duration.ZERO);

        verify(valueOps).set(PREFIX + "k", "v");
    }

    @Test
    void putWithNullTtlSetsKeyWithoutExpiry() {
        store.put("k", "v", null);

        verify(valueOps).set(PREFIX + "k", "v");
    }

    @Test
    void sizeIsNotReported() {
        assertThat(store.size()).isEqualTo(-1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidateAllScansAndDeletesEveryCourierKey() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(PREFIX + "a", PREFIX + "b");
        when(template.scan(any(ScanOptions.class))).thenReturn(cursor);

        store.invalidateAll();

        verify(template).delete(List.of(PREFIX + "a", PREFIX + "b"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidateByPrefixDeletesOnlyThatRequestType() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(PREFIX + "com.example.Query:1");
        when(template.scan(any(ScanOptions.class))).thenReturn(cursor);

        store.invalidateByPrefix("com.example.Query:");

        verify(template).delete(List.of(PREFIX + "com.example.Query:1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deletingNothingIssuesNoDelete() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);
        when(template.scan(any(ScanOptions.class))).thenReturn(cursor);

        store.invalidateAll();

        verify(template, org.mockito.Mockito.never()).delete(org.mockito.ArgumentMatchers.anyCollection());
    }
}
