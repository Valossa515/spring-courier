package io.github.valossa515.spring_courier.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedisKeysTest {

    @Test
    void leavesPlainTextUntouched() {
        assertThat(RedisKeys.escapeGlob("courier:cache:com.example.Query:"))
                .isEqualTo("courier:cache:com.example.Query:");
    }

    @Test
    void escapesGlobMetacharacters() {
        assertThat(RedisKeys.escapeGlob("a*b?c[d]e\\f"))
                .isEqualTo("a\\*b\\?c\\[d\\]e\\\\f");
    }

    @Test
    void handlesEmptyText() {
        assertThat(RedisKeys.escapeGlob("")).isEmpty();
    }
}
