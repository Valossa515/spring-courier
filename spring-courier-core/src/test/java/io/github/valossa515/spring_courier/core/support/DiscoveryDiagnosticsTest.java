package io.github.valossa515.spring_courier.core.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import io.github.valossa515.spring_courier.core.interfaces.ICommand;
import io.github.valossa515.spring_courier.core.interfaces.INotification;
import io.github.valossa515.spring_courier.core.interfaces.IRequest;
import io.github.valossa515.spring_courier.core.interfaces.NotificationHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * A handler that is detected but never registered fails far from its cause: a
 * request handler surfaces as {@code HandlerNotFoundException} on the first
 * dispatch, and a notification handler does not surface at all — the event is
 * dropped silently. The startup warning is the only place the real reason can
 * be stated, so these tests pin that it actually is.
 */
class DiscoveryDiagnosticsTest {

    private ListAppender<ILoggingEvent> handlerLog;
    private ListAppender<ILoggingEvent> notificationLog;

    private HandlerDiscoveryPostProcessor handlerDiscovery;
    private NotificationDiscoveryPostProcessor notificationDiscovery;

    @BeforeEach
    void setUp() {
        handlerLog = attachTo(HandlerDiscoveryPostProcessor.class);
        notificationLog = attachTo(NotificationDiscoveryPostProcessor.class);
        handlerDiscovery = new HandlerDiscoveryPostProcessor(
                new HandlerRegistry(), mock(ApplicationContext.class));
        notificationDiscovery = new NotificationDiscoveryPostProcessor(
                new NotificationRegistry(), mock(ApplicationContext.class));
    }

    @AfterEach
    void tearDown() {
        detach(HandlerDiscoveryPostProcessor.class, handlerLog);
        detach(NotificationDiscoveryPostProcessor.class, notificationLog);
    }

    private static ListAppender<ILoggingEvent> attachTo(Class<?> type) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(type)).addAppender(appender);
        return appender;
    }

    private static void detach(Class<?> type, ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(type)).detachAppender(appender);
    }

    private String warningFrom(ListAppender<ILoggingEvent> log) {
        return log.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .reduce((first, second) -> first + " | " + second)
                .orElse("");
    }

    // ── Fixtures ────────────────────────────────────────────────────────

    record SampleCommand(String id) implements ICommand<String> { }

    record Wrapped<T>(T value) implements ICommand<String> { }

    interface Audited extends INotification { }

    /** A catch-all handler: detected by duck-typing, impossible to key on. */
    interface CatchAllHandler {
        Object handle(IRequest<?> request);
    }

    static class CatchAll implements CatchAllHandler {
        @Override
        public Object handle(IRequest<?> request) {
            return null;
        }
    }

    /** The request type never gets fixed to a class. */
    static class UnboundHandler<C extends IRequest<String>> implements CommandHandler<C, String> {
        @Override
        public String handle(C command) {
            return null;
        }
    }

    /** The request type is generic, so erasure loses the argument. */
    static class GenericRequestHandler implements CommandHandler<Wrapped<String>, String> {
        @Override
        public String handle(Wrapped<String> command) {
            return null;
        }
    }

    static class GenericNotificationHandler<T extends Audited> implements NotificationHandler<T> {
        @Override
        public void handle(T notification) {
            // no-op
        }
    }

    static class InterfaceNotificationHandler implements NotificationHandler<Audited> {
        @Override
        public void handle(Audited notification) {
            // no-op
        }
    }

    // ── Request handlers ────────────────────────────────────────────────

    @Test
    void namesTheTypeVariableWhenTheRequestTypeWasNeverFixed() {
        handlerDiscovery.postProcessAfterInitialization(new UnboundHandler<SampleCommand>(), "h");

        assertThat(warningFrom(handlerLog))
                .contains("NOT registered")
                .contains("type variable 'C'")
                .contains("HandlerNotFoundException");
    }

    @Test
    void saysErasureIsTheReasonForAGenericRequest() {
        handlerDiscovery.postProcessAfterInitialization(new GenericRequestHandler(), "h");

        assertThat(warningFrom(handlerLog))
                .contains("is generic")
                .contains("erased")
                .contains("concrete request class");
    }

    @Test
    void explainsThatACatchAllHandlerHasNoRequestTypeToKeyOn() {
        handlerDiscovery.postProcessAfterInitialization(new CatchAll(), "h");

        assertThat(warningFrom(handlerLog))
                .as("duck-typing detects it, but the registry is keyed by exact request class")
                .contains("no request type argument");
    }

    // ── Notification handlers ───────────────────────────────────────────

    @Test
    void namesTheTypeVariableForAGenericNotificationHandler() {
        notificationDiscovery.postProcessAfterInitialization(
                new GenericNotificationHandler<>(), "h");

        assertThat(warningFrom(notificationLog))
                .contains("type variable 'T'")
                .contains("dropped silently");
    }

    @Test
    void explainsWhyAnInterfaceNotificationTypeCannotBeRegistered() {
        notificationDiscovery.postProcessAfterInitialization(
                new InterfaceNotificationHandler(), "h");

        assertThat(warningFrom(notificationLog))
                .contains("Audited")
                .contains("an interface")
                .contains("dispatched by exact class");
    }

    @Test
    void aGenericHandlerIsReportedOnceRatherThanTwice() {
        notificationDiscovery.postProcessAfterInitialization(
                new GenericNotificationHandler<>(), "h");

        assertThat(notificationLog.list.stream().filter(e -> e.getLevel() == Level.WARN).count())
                .as("the per-cause warning and the summary used to both fire for one bean")
                .isEqualTo(1);
    }
}
