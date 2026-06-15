package io.github.valossa515.spring_courier.outbox;

import io.github.valossa515.spring_courier.core.Courier;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Background worker that drains the outbox: it reclaims crashed-in-flight
 * messages, fetches pending ones, claims each atomically and dispatches it to
 * the Courier notification handlers.
 *
 * <p>Delivery is <strong>at-least-once</strong>: a crash between a successful
 * dispatch and the {@code PROCESSED} write causes redelivery, so notification
 * handlers must be idempotent.
 *
 * <p>Runs on its own single-threaded scheduler managed via {@link SmartLifecycle},
 * so the host application does not need {@code @EnableScheduling}.
 */
public class OutboxPoller implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxStore store;
    private final OutboxSerializer serializer;
    private final Courier courier;
    private final OutboxProperties properties;

    private ScheduledExecutorService scheduler;
    private volatile boolean running;

    public OutboxPoller(OutboxStore store, OutboxSerializer serializer,
            Courier courier, OutboxProperties properties) {
        this.store = store;
        this.serializer = serializer;
        this.courier = courier;
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "courier-outbox-poller");
            t.setDaemon(true);
            return t;
        });
        long delay = properties.getPollDelayMs();
        scheduler.scheduleWithFixedDelay(this::pollSafely, delay, delay, TimeUnit.MILLISECONDS);
        running = true;
        LOG.info("Outbox poller started (every {} ms, batch={}, maxAttempts={})",
                delay, properties.getBatchSize(), properties.getMaxAttempts());
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** Wraps a poll cycle so an error never cancels the recurring schedule. */
    private void pollSafely() {
        try {
            pollOnce();
        } catch (Exception e) {
            LOG.warn("Outbox poll cycle failed; will retry next cycle", e);
        }
    }

    /**
     * Runs a single drain cycle. Visible for testing.
     */
    void pollOnce() {
        int reclaimed = store.reclaimStale(Duration.ofMillis(properties.getProcessingTimeoutMs()));
        if (reclaimed > 0) {
            LOG.info("Reclaimed {} stale outbox message(s)", reclaimed);
        }
        List<OutboxMessage> batch = store.fetchPending(properties.getBatchSize());
        for (OutboxMessage message : batch) {
            if (store.claim(message.id())) {
                dispatch(message);
            }
        }
    }

    private void dispatch(OutboxMessage message) {
        try {
            INotification event = serializer.deserialize(message.eventType(), message.payload());
            courier.publish(event);
            store.markProcessed(message.id());
            LOG.debug("Dispatched outbox message {} ({})", message.id(), message.eventType());
        } catch (Exception e) {
            LOG.warn("Failed to dispatch outbox message {} ({}); attempt will be recorded",
                    message.id(), message.eventType(), e);
            store.markFailed(message.id(), String.valueOf(e), properties.getMaxAttempts());
        }
    }
}
