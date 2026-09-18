package com.sitionix.forgeit.mockmvc.internal.domain;

import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.mockmvc.internal.executor.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorTimingTest {
    static MockMvcRequest request(String path) {
        return new MockMvcRequest(HttpMethod.POST, path, Map.of(), Map.of(), Map.of(), Map.of(), null);
    }

    @Test void mvcReturnsPositiveElapsedAfterCompleteBody() throws Exception {
        var executor = new MvcExecutor(MockMvcBuilders.standaloneSetup(new MockMvcTimingTest.TimingController()).build());
        var response = executor.execute(request("/timing/example"));
        assertEquals(200, response.status());
        assertEquals("{\"secret\":\"response-secret\"}", response.body());
        assertTrue(response.elapsed().isPositive());
    }

    @Test void mvcAsyncDurationIncludesWaitingForBody() throws Exception {
        var controller = new AsyncController();
        var executor = new MvcExecutor(MockMvcBuilders.standaloneSetup(controller).build());
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var result = workers.submit(() -> executor.execute(request("/async")));
            try {
                assertTrue(controller.entered.await(3, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> result.get(30, TimeUnit.MILLISECONDS));
                controller.body.setResult("complete");
                var response = result.get(3, TimeUnit.SECONDS);
                assertEquals("complete", response.body());
                assertTrue(response.elapsed().compareTo(Duration.ofMillis(30)) >= 0);
            } finally { controller.body.setResult("complete"); }
        }
    }

    @Test void httpDurationIncludesBlockedBodyAndReturnsItInFull() throws Exception {
        try (var harness = new BodyServer(); var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var result = workers.submit(() -> harness.executor(Duration.ofSeconds(5)).execute(request("/body")));
            try {
                assertTrue(harness.headersSent.await(3, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> result.get(30, TimeUnit.MILLISECONDS));
                harness.release.countDown();
                var response = result.get(3, TimeUnit.SECONDS);
                assertEquals(200, response.status());
                assertEquals("{}", response.body());
                assertTrue(response.elapsed().compareTo(Duration.ofMillis(30)) >= 0);
            } finally { harness.release.countDown(); }
        }
    }

    @Test void performanceFailureWaitsForValidResponseAndDoesNotChangeTransportTimeout() throws Exception {
        try (var harness = new BodyServer(); var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var builder = MockMvcTimingTest.builder(harness.executor(Duration.ofSeconds(5)), "/body")
                    .expectResponseWithin(Duration.ofNanos(1));
            var result = workers.submit(() -> assertThrows(AssertionError.class, builder::assertDefault));
            try {
                assertTrue(harness.headersSent.await(3, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> result.get(30, TimeUnit.MILLISECONDS));
                harness.release.countDown();
                var error = result.get(3, TimeUnit.SECONDS);
                assertTrue(error.getMessage().contains("expected maximum duration"));
                assertTrue(error.getMessage().contains("actual duration"));
                assertEquals(0, harness.bodySent.getCount());
            } finally { harness.release.countDown(); }
        }
    }

    @Test void generousPerformanceLimitCannotDisableHardRequestTimeout() throws Exception {
        try (var harness = new BodyServer()) {
            var builder = MockMvcTimingTest.builder(harness.executor(Duration.ofSeconds(1)), "/body")
                    .expectResponseWithin(Duration.ofSeconds(20));
            var error = assertTimeoutPreemptively(Duration.ofSeconds(4), () ->
                    assertThrows(IllegalStateException.class, builder::assertDefault));
            assertTrue(error.getMessage().toLowerCase().contains("timeout"));
            assertEquals(0, harness.headersSent.getCount());
            assertEquals(1, harness.release.getCount());
        }
    }

    @RestController static class AsyncController {
        final CountDownLatch entered = new CountDownLatch(1);
        final DeferredResult<String> body = new DeferredResult<>(5000L);
        @PostMapping("/async") DeferredResult<String> respond() { entered.countDown(); return body; }
    }

    static final class BodyServer implements AutoCloseable {
        final CountDownLatch headersSent = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch bodySent = new CountDownLatch(1);
        final HttpServer server;
        final HttpClient client = HttpClient.newHttpClient();
        BodyServer() throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try {
                    exchange.getRequestBody().readAllBytes();
                    exchange.sendResponseHeaders(200, 2);
                    exchange.getResponseBody().write('{');
                    exchange.getResponseBody().flush();
                    headersSent.countDown();
                    if (release.await(5, TimeUnit.SECONDS)) {
                        exchange.getResponseBody().write('}');
                        bodySent.countDown();
                    }
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            });
            server.start();
        }
        HttpExecutor executor(Duration timeout) {
            return new HttpExecutor(client, URI.create("http://127.0.0.1:" + server.getAddress().getPort()), timeout);
        }
        public void close() { release.countDown(); client.shutdownNow(); server.stop(0); }
    }
}
