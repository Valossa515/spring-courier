package io.github.valossa515.spring_courier.cache.redis;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

/**
 * Key-space helpers shared by the Redis-backed stores.
 */
final class RedisKeys {

    /** Keys fetched per SCAN round trip, and per DEL batch. */
    private static final int BATCH_SIZE = 500;

    private RedisKeys() {
    }

    /**
     * Deletes every key starting with {@code literalPrefix}.
     *
     * <p>Uses {@code SCAN} rather than {@code KEYS} so a large keyspace does
     * not block the Redis server, and deletes in batches.
     *
     * @param template     template used to talk to Redis
     * @param literalPrefix prefix to match, treated as a literal (glob
     *                      metacharacters in it are escaped)
     */
    static void deleteByPattern(RedisTemplate<String, Object> template, String literalPrefix) {
        String pattern = escapeGlob(literalPrefix) + "*";
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(BATCH_SIZE)
                .build();

        List<String> batch = new ArrayList<>(BATCH_SIZE);
        try (Cursor<String> cursor = template.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= BATCH_SIZE) {
                    template.delete(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            template.delete(batch);
        }
    }

    /**
     * Escapes Redis glob metacharacters so the given text matches literally.
     */
    static String escapeGlob(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '*' || c == '?' || c == '[' || c == ']' || c == '\\') {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
