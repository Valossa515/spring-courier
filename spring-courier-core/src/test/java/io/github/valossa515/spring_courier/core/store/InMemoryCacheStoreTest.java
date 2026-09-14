package io.github.valossa515.spring_courier.core.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class InMemoryCacheStoreTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    @Test
    void storesAndRetrievesValue() {
        InMemoryCacheStore store = new InMemoryCacheStore(0);

        store.put("k", "v", ONE_MINUTE);

        assertThat(store.get("k")).contains("v");
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    void returnsEmptyForUnknownKey() {
        assertThat(new InMemoryCacheStore(0).get("missing")).isEmpty();
    }

    @Test
    void treatsExpiredEntryAsMissAndDropsIt() throws InterruptedException {
        InMemoryCacheStore store = new InMemoryCacheStore(0);

        store.put("k", "v", Duration.ofMillis(1));
        Thread.sleep(10);

        assertThat(store.get("k")).isEmpty();
        assertThat(store.size()).isZero();
    }

    @Test
    void nonPositiveTtlMeansNoExpiry() {
        InMemoryCacheStore store = new InMemoryCacheStore(0);

        store.put("zero", "v", Duration.ZERO);
        store.put("nullTtl", "v", null);

        assertThat(store.get("zero")).contains("v");
        assertThat(store.get("nullTtl")).contains("v");
    }

    @Test
    void dropsNewEntryWhenFullOfLiveEntries() {
        InMemoryCacheStore store = new InMemoryCacheStore(2);

        store.put("a", 1, ONE_MINUTE);
        store.put("b", 2, ONE_MINUTE);
        store.put("c", 3, ONE_MINUTE);

        assertThat(store.size()).isEqualTo(2);
        assertThat(store.get("c")).isEmpty();
        assertThat(store.get("a")).contains(1);
    }

    @Test
    void reclaimsRoomByEvictingExpiredEntries() throws InterruptedException {
        InMemoryCacheStore store = new InMemoryCacheStore(2);

        store.put("stale", 1, Duration.ofMillis(1));
        Thread.sleep(10);
        store.put("live", 2, ONE_MINUTE);
        store.put("new", 3, ONE_MINUTE);

        assertThat(store.get("new")).contains(3);
        assertThat(store.get("live")).contains(2);
        assertThat(store.get("stale")).isEmpty();
    }

    @Test
    void invalidateAllRemovesEverything() {
        InMemoryCacheStore store = new InMemoryCacheStore(0);
        store.put("a", 1, ONE_MINUTE);
        store.put("b", 2, ONE_MINUTE);

        store.invalidateAll();

        assertThat(store.size()).isZero();
    }

    @Test
    void invalidateByPrefixRemovesOnlyMatchingKeys() {
        InMemoryCacheStore store = new InMemoryCacheStore(0);
        store.put("com.example.Query:1", 1, ONE_MINUTE);
        store.put("com.example.Query:2", 2, ONE_MINUTE);
        store.put("com.example.Other:1", 3, ONE_MINUTE);

        store.invalidateByPrefix("com.example.Query:");

        assertThat(store.size()).isEqualTo(1);
        assertThat(store.get("com.example.Other:1")).contains(3);
    }
}
