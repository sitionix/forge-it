package com.sitionix.forgeit.mockmvc.internal.domain;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.*;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;
import com.sitionix.forgeit.mockmvc.api.*;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import com.sitionix.forgeit.mockmvc.internal.executor.*;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockMvcTimingTest {
    @Test void assertionIsOptionalAndUsesInclusiveNanosecondPrecision() {
        builder(request -> new MockMvcResponse(200, "{}", Duration.ofDays(30)), "/timing").assertDefault();
        for (Duration limit : List.of(Duration.ofNanos(21), Duration.ofNanos(20), Duration.ofSeconds(Long.MAX_VALUE))) {
            builder(request -> new MockMvcResponse(200, "{}", Duration.ofNanos(20)), "/timing")
                    .expectResponseWithin(limit).assertDefault();
        }
        var error = assertThrows(AssertionError.class, () ->
                builder(request -> new MockMvcResponse(200, "{}", Duration.ofNanos(21)), "/timing")
                        .expectResponseWithin(Duration.ofNanos(20)).assertDefault());
        assertTrue(error.getMessage().contains("expected maximum duration: PT0.00000002S"));
        assertTrue(error.getMessage().contains("actual duration: PT0.000000021S"));
    }

    @Test void rejectsNullZeroAndNegativeThresholdBeforeExecuting() {
        for (Duration limit : new Duration[]{null, Duration.ZERO, Duration.ofNanos(-1)}) {
            var builder = builder(request -> { fail("Must validate before IO"); return null; }, "/timing");
            assertThrows(IllegalArgumentException.class, () -> builder.expectResponseWithin(limit));
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void logsOneSafeLineForReceivedResponseEvenWhenTimingAssertionFails(boolean http) throws Exception {
        try (var capture = new TimingLog(); var client = HttpClient.newHttpClient()) {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                byte[] body = "{\"secret\":\"response-secret\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            try {
                MockMvcExecutor executor = http
                        ? new HttpExecutor(client, URI.create("http://127.0.0.1:" + server.getAddress().getPort()), Duration.ofSeconds(5))
                        : new MvcExecutor(MockMvcBuilders.standaloneSetup(new TimingController()).build());
                assertThrows(AssertionError.class, () -> builder(executor, "/timing/{id}")
                        .withPathParameters(PathParams.create().add("id", "path-secret"))
                        .withQueryParameters(QueryParams.create().add("query", "query-secret"))
                        .header("X-Private", "header-secret").token("Bearer token-secret")
                        .cookie("session", "cookie-secret").withRequest("request.json")
                        .expectResponseWithin(Duration.ofNanos(1)).assertDefault());
                var messages = capture.messages();
                assertEquals(1, messages.size());
                assertTrue(messages.getFirst().matches("HTTP POST /timing/\\{id} status=200 duration=\\d+ms"), messages.toString());
                for (String secret : List.of("127.0.0.1", "path-secret", "query-secret", "X-Private", "header-secret",
                        "Authorization", "Bearer", "token-secret", "session", "cookie-secret", "request-secret", "response-secret")) {
                    assertFalse(messages.getFirst().contains(secret), secret);
                }
            } finally { server.stop(0); }
        }
    }

    @Test void legacyMvcUrlTemplateCannotLeakAuthorityInlineQueryOrLogNewlines() {
        try (var capture = new TimingLog()) {
            builder(request -> new MockMvcResponse(200, "{}", Duration.ofMillis(3)),
                    "https://private-host/timing/{id}?token=query-secret#fragment-secret").assertDefault();
            assertEquals(List.of("HTTP POST /timing/{id} status=200 duration=3ms"), capture.messages());
            capture.appender.list.clear();
            builder(request -> new MockMvcResponse(200, "{}", Duration.ofMillis(3)), "/timing/\n\r\u2028").assertDefault();
            assertEquals(List.of("HTTP POST /timing/___ status=200 duration=3ms"), capture.messages());
        }
    }

    @Test void responseIsLoggedBeforeStatusFailureButTransportFailureHasNoResponseLog() {
        try (var capture = new TimingLog()) {
            assertThrows(AssertionError.class, () -> builder(request -> new MockMvcResponse(503, "{}", Duration.ofMillis(3)),
                    "/timing").assertDefault());
            assertEquals(List.of("HTTP POST /timing status=503 duration=3ms"), capture.messages());
            capture.appender.list.clear();
            assertThrows(IllegalStateException.class, () -> builder(request -> {
                throw new IllegalStateException("transport failed");
            }, "/timing").assertDefault());
            assertTrue(capture.messages().isEmpty());
        }
    }

    static MockMvcBuilder<String, String> builder(MockMvcExecutor executor, String path) {
        var properties = new MockMvcProperties();
        var paths = new MockMvcProperties.Path();
        paths.setRequest("/request"); paths.setDefaultResponse("/response");
        properties.setPath(paths);
        var fixtures = new MockMvcBuilderDefaultsTest.FakeJsonLoader();
        fixtures.put("/request/request.json", "{\"secret\":\"request-secret\"}");
        fixtures.put("/response/response.json", "{}");
        return new MockMvcBuilder<>(executor, new MockMvcLoader(fixtures, properties), new ObjectMapper(),
                Endpoint.createContract(path, HttpMethod.POST, String.class, String.class,
                        (MockmvcDefault) defaults -> defaults.expectStatus(200).expectResponse("response.json")));
    }

    @RestController static class TimingController {
        @PostMapping("/timing/{id}") String respond() { return "{\"secret\":\"response-secret\"}"; }
    }

    static final class TimingLog implements AutoCloseable {
        final Logger logger = (Logger) LoggerFactory.getLogger(MockMvcBuilder.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        TimingLog() { appender.start(); logger.addAppender(appender); }
        List<String> messages() { return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList(); }
        public void close() { logger.detachAppender(appender); appender.stop(); }
    }
}
