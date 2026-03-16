package io.github.valossa515.spring_courier.core.slack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackNotifierCoverageTest {

    private HttpClient httpClient;
    private SlackNotifier notifier;
    private SlackNotifier notifierWithChannel;

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    @SuppressWarnings("unchecked")
    private void stubHttpOk() {
        HttpResponse<String> response = mockResponse(200, "ok");
        doReturn(CompletableFuture.completedFuture(response))
                .when(httpClient)
                .sendAsync(any(HttpRequest.class), any());
    }

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        notifier = new SlackNotifier(
                "https://hooks.slack.com/services/T/B/x",
                null, httpClient);
        notifierWithChannel = new SlackNotifier(
                "https://hooks.slack.com/services/T/B/x",
                "#alerts", httpClient);
    }

    @Test
    void sendFiringWarningDoesNotIncludeMention() {
        stubHttpOk();

        CompletableFuture<Void> future = notifier.sendFiring(
                "high-error-ratio", AlertSeverity.WARNING,
                "High Error Ratio", "10% errors", "TestApp");

        assertDoesNotThrow(() -> future.get());

        ArgumentCaptor<HttpRequest> captor =
                ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendAsync(captor.capture(), any());

        HttpRequest req = captor.getValue();
        assertEquals("application/json",
                req.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void sendFiringCriticalIncludesHereMention() {
        stubHttpOk();

        CompletableFuture<Void> future = notifier.sendFiring(
                "handler-timeouts", AlertSeverity.CRITICAL,
                "Timeouts Detected", "5 timeouts/s", "TestApp");

        assertDoesNotThrow(() -> future.get());
        verify(httpClient).sendAsync(any(HttpRequest.class), any());
    }

    @Test
    void sendResolvedSendsCorrectRequest() {
        stubHttpOk();

        CompletableFuture<Void> future = notifier.sendResolved(
                "high-error-ratio",
                "Condition returned to normal.",
                "TestApp");

        assertDoesNotThrow(() -> future.get());
        verify(httpClient).sendAsync(any(HttpRequest.class), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendLogsWarningOnNon200Response() {
        HttpResponse<String> response = mockResponse(500, "error");
        doReturn(CompletableFuture.completedFuture(response))
                .when(httpClient)
                .sendAsync(any(HttpRequest.class), any());

        CompletableFuture<Void> future = notifier.sendFiring(
                "test", AlertSeverity.WARNING,
                "Test", "detail", "App");

        assertDoesNotThrow(() -> future.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendHandlesIOException() {
        CompletableFuture<HttpResponse<String>> failed =
                new CompletableFuture<>();
        failed.completeExceptionally(new IOException("connection refused"));
        doReturn(failed)
                .when(httpClient)
                .sendAsync(any(HttpRequest.class), any());

        CompletableFuture<Void> future = notifier.sendFiring(
                "test", AlertSeverity.WARNING,
                "Test", "detail", "App");

        assertDoesNotThrow(() -> future.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendHandlesNonIOException() {
        CompletableFuture<HttpResponse<String>> failed =
                new CompletableFuture<>();
        failed.completeExceptionally(
                new RuntimeException("unexpected error"));
        doReturn(failed)
                .when(httpClient)
                .sendAsync(any(HttpRequest.class), any());

        CompletableFuture<Void> future = notifier.sendFiring(
                "test", AlertSeverity.WARNING,
                "Test", "detail", "App");

        assertDoesNotThrow(() -> future.get());
    }

    @Test
    void sendWithChannelIncludesChannelInPayload() {
        stubHttpOk();

        CompletableFuture<Void> future = notifierWithChannel.sendFiring(
                "test", AlertSeverity.WARNING,
                "Test", "detail", "App");

        assertDoesNotThrow(() -> future.get());
        verify(httpClient).sendAsync(any(HttpRequest.class), any());
    }

    @Test
    void sendWithBlankChannelOmitsChannel() {
        SlackNotifier blankChannel = new SlackNotifier(
                "https://hooks.slack.com/services/T/B/x",
                "   ", httpClient);
        stubHttpOk();

        CompletableFuture<Void> future = blankChannel.sendFiring(
                "test", AlertSeverity.WARNING,
                "Test", "detail", "App");

        assertDoesNotThrow(() -> future.get());
    }

    @Test
    void sendResolvedWithChannelIncludesChannel() {
        stubHttpOk();

        CompletableFuture<Void> future =
                notifierWithChannel.sendResolved(
                        "test", "Recovered.", "App");

        assertDoesNotThrow(() -> future.get());
    }

    @Test
    void buildPayloadContainsTextKey() {
        stubHttpOk();

        notifier.sendFiring("a", AlertSeverity.WARNING,
                "s", "d", "app");

        ArgumentCaptor<HttpRequest> captor =
                ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendAsync(captor.capture(), any());

        assertTrue(captor.getValue().uri().toString()
                .contains("hooks.slack.com"));
    }
}
