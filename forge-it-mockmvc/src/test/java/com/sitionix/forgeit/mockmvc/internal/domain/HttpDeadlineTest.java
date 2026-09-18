package com.sitionix.forgeit.mockmvc.internal.domain;

import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.mockmvc.internal.executor.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpDeadlineTest {
    private static MockMvcRequest request() {
        return new MockMvcRequest(HttpMethod.GET, "/stall", Map.of(), Map.of(), Map.of(), Map.of(), null);
    }
    @Test void rejectsUnboundedAndNonpositiveTimeouts() {
        for (Duration value : new Duration[]{null, Duration.ZERO, Duration.ofMillis(-1), Duration.ofDays(1)}) {
            assertThrows(IllegalArgumentException.class, () -> HttpExecutor.requireTimeout(value, "request-timeout"));
        }
    }
    @Test void deadlineIncludesStalledResponseBodyWithoutRetry() throws Exception {
        try (var harness = new StalledServer()) {
            var failure = assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                    assertThrows(IllegalStateException.class, () -> harness.executor(Duration.ofSeconds(1)).execute(request())));
            assertTrue(harness.entered.await(1, TimeUnit.SECONDS));
            assertTrue(failure.getMessage().contains("timeout"));
            assertEquals(1, harness.requests.get());
        }
    }
    @Test void cancellationPreservesInterruptAndDoesNotRetry() throws Exception {
        try (var harness = new StalledServer()) {
            var interrupted = new AtomicBoolean();
            var failure = new AtomicReference<Throwable>();
            Thread worker = Thread.ofVirtual().start(() -> {
                try { harness.executor(Duration.ofSeconds(5)).execute(request()); }
                catch (Throwable ex) { failure.set(ex); interrupted.set(Thread.currentThread().isInterrupted()); }
            });
            try {
                assertTrue(harness.entered.await(3, TimeUnit.SECONDS));
                worker.interrupt(); worker.join(3000);
                assertFalse(worker.isAlive());
                assertTrue(interrupted.get());
                assertInstanceOf(IllegalStateException.class, failure.get());
                assertTrue(failure.get().getMessage().contains("interrupted"));
                assertEquals(1, harness.requests.get());
            } finally { worker.interrupt(); }
        }
    }
    static class StalledServer implements AutoCloseable {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger requests = new AtomicInteger();
        final HttpServer server;
        final HttpClient client;
        StalledServer() throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                requests.incrementAndGet();
                try {
                    exchange.sendResponseHeaders(200, 100);
                    exchange.getResponseBody().write('a'); exchange.getResponseBody().flush();
                    entered.countDown();
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            });
            server.start();
            client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
        }
        HttpExecutor executor(Duration timeout) {
            return new HttpExecutor(client, URI.create("http://127.0.0.1:" + server.getAddress().getPort()), timeout);
        }
        public void close() { release.countDown(); client.shutdownNow(); server.stop(0); }
    }
}
