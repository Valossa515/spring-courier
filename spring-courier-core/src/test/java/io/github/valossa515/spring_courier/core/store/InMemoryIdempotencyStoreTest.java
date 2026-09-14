package io.github.valossa515.spring_courier.core.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class InMemoryIdempotencyStoreTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    @Test
    void storesAndRetrievesRecordedResult() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(0);

        store.put("k", "result", ONE_MINUTE);

        assertThat(store.get("k")).contains("result");
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    void returnsEmptyForUnknownKey() {
        assertThat(new InMemoryIdempotencyStore(0).get("missing")).isEmpty();
    }

    @Test
    void treatsExpiredEntryAsMissAndDropsIt() throws InterruptedException {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(0);

        store.put("k", "v", Duration.ofMillis(1));
        Thread.sleep(10);

        assertThat(store.get("k")).isEmpty();
        assertThat(store.size()).isZero();
    }

    @Test
    void nonPositiveTtlMeansNoExpiry() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(0);

        store.put("zero", "v", Duration.ZERO);
        store.put("nullTtl", "v", null);

        assertThat(store.get("zero")).contains("v");
        assertThat(store.get("nullTtl")).contains("v");
    }

    @Test
    void dropsNewEntryWhenFullOfLiveEntries() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(1);

        store.put("a", 1, ONE_MINUTE);
        store.put("b", 2, ONE_MINUTE);

        assertThat(store.size()).isEqualTo(1);
        assertThat(store.get("b")).isEmpty();
    }

    @Test
    void reclaimsRoomByEvictingExpiredEntries() throws InterruptedException {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(1);

        store.put("stale", 1, Duration.ofMillis(1));
        Thread.sleep(10);
        store.put("new", 2, ONE_MINUTE);

        assertThat(store.get("new")).contains(2);
    }

    @Test
    void removeDropsSingleEntry() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(0);
        store.put("a", 1, ONE_MINUTE);
        store.put("b", 2, ONE_MINUTE);

        store.remove("a");

        assertThat(store.get("a")).isEmpty();
        assertThat(store.get("b")).contains(2);
    }

    @Test
    void clearRemovesEverything() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(0);
        store.put("a", 1, ONE_MINUTE);

        store.clear();

        assertThat(store.size()).isZero();
    }
}
