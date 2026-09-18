package com.sitionix.forgeit.mockmvc.internal.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.*;
import com.sitionix.forgeit.mockmvc.api.*;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import com.sitionix.forgeit.mockmvc.internal.executor.*;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class HttpExecutorTest {
    private HttpServer server;
    private HttpClient client;
    private URI base;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] body = exchange.getRequestBody().readAllBytes();
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("redirect")) {
                exchange.getResponseHeaders().add("Location", "/api/target");
                exchange.sendResponseHeaders(302, -1);
            } else if (path.endsWith("empty")) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(path.endsWith("error") ? 500 : path.endsWith("bad") ? 400 : 200, body.length == 0 ? -1 : body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(1)).build();
    }
    @AfterEach void stop() { client.shutdownNow(); server.stop(0); }

    private MockMvcBuilder<String,String> builder(String path, HttpMethod method) {
        var properties = new MockMvcProperties();
        var paths = new MockMvcProperties.Path();
        paths.setRequest("/request"); paths.setResponse("/response");
        paths.setDefaultRequest("/request"); paths.setDefaultResponse("/response");
        properties.setPath(paths);
        var fixtures = new MockMvcBuilderDefaultsTest.FakeJsonLoader();
        fixtures.put("/request/body.json", "{\"text\":\"Привіт\"}");
        fixtures.put("/response/body.json", "{\"text\":\"Привіт\"}");
        return new MockMvcBuilder<>(new HttpExecutor(client, base, Duration.ofSeconds(2)),
                new MockMvcLoader(fixtures, properties), new ObjectMapper(),
                Endpoint.createContract(path, method, String.class, String.class,
                        (com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault) c -> c.withRequest("body.json").expectResponse("body.json").expectStatus(200)));
    }
    @Test void allMethodsUseExistingDefaultFixturesAndPortableAssertions() {
        for (var method : new HttpMethod[]{HttpMethod.GET,HttpMethod.POST,HttpMethod.PUT,HttpMethod.PATCH,HttpMethod.DELETE}) {
            builder("/echo", method).assertDefault();
        }
        assertEquals(5, requests.get());
    }
    @Test void preservesErrorResponsesAndDoesNotFollowRedirects() {
        builder("/error", HttpMethod.POST).expectStatus(HttpStatus.INTERNAL_SERVER_ERROR).assertDefault();
        builder("/bad", HttpMethod.POST).expectStatus(HttpStatus.BAD_REQUEST).assertDefault();
        builder("/redirect", HttpMethod.GET).expectStatus(HttpStatus.FOUND).assertAndCreate();
        builder("/empty", HttpMethod.DELETE).expectStatus(HttpStatus.NO_CONTENT).assertAndCreate();
        assertEquals(4, requests.get());
    }
    @Test void mismatchIsAssertionFailureAndMatcherIsRejectedBeforeIo() {
        assertThrows(AssertionError.class, () -> builder("/empty", HttpMethod.GET)
                .expectStatus(HttpStatus.OK).assertAndCreate());
        assertThrows(IllegalStateException.class, () -> builder("/echo", HttpMethod.GET).andExpectPath(r -> {}));
        assertEquals(1, requests.get());
    }
    @Test void invalidHeadersDoNotExposeSecrets() {
        var error = assertThrows(IllegalArgumentException.class, () -> builder("/echo", HttpMethod.GET)
                .token("secret-token\ninvalid").cookie("session", "secret-cookie\ninvalid").assertAndCreate());
        assertFalse(error.toString().contains("secret"));
        assertNull(error.getCause());
        assertEquals(0, requests.get());
    }
    @Test void connectionFailureReportsCauseAndMethodWithoutRequestSecrets() {
        server.stop(0);
        var error = assertThrows(IllegalStateException.class, () -> builder("/private", HttpMethod.POST)
                .token("secret-token").header("X-Secret", "secret-header").assertAndCreate());
        assertTrue(error.getMessage().contains("HTTP POST"));
        assertTrue(error.getMessage().contains("ConnectException"));
        assertFalse(error.getMessage().contains("secret"));
        assertNull(error.getCause());
    }
    @Test void endpointCannotReplaceOrigin() {
        for (String path : new String[]{"http://elsewhere/", "//elsewhere/"}) {
            assertThrows(IllegalArgumentException.class, () -> builder(path, HttpMethod.GET).assertAndCreate());
        }
        assertEquals(0, requests.get());
    }
}
