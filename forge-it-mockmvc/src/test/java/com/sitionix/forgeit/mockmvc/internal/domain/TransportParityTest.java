package com.sitionix.forgeit.mockmvc.internal.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.*;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.mockmvc.api.*;
import com.sitionix.forgeit.mockmvc.internal.config.MockMvcProperties;
import com.sitionix.forgeit.mockmvc.internal.journal.MockMvcJournal;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.assertj.core.api.Assertions.*;

class TransportParityTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Endpoint<Payload, Payload> ECHO = Endpoint.createContract("/echo/{id}", HttpMethod.POST,
            Payload.class, Payload.class, (MockmvcDefault) c -> c.withRequest("body.json").expectResponse("body.json")
                    .expectStatus(200).token("endpoint-token").header("X-Default", "endpoint").cookie("session", "default"));
    private final List<Seen> received = new CopyOnWriteArrayList<>();
    private final List<HttpServer> servers = new ArrayList<>();
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final MockEnvironment environment = new MockEnvironment();

    @AfterEach void close() { client.shutdownNow(); servers.forEach(server -> server.stop(0)); }

    private String server() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.add(new Seen(exchange.getRequestURI().toASCIIString(), exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("X-Default"), exchange.getRequestHeaders().getFirst("Cookie"), body,
                    server.getAddress().getPort()));
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start(); servers.add(server);
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api";
    }

    private MockMvcJournal journal(boolean http) throws Exception {
        var props = new MockMvcProperties();
        var paths = new MockMvcProperties.Path();
        paths.setRequest("/explicit/request"); paths.setResponse("/explicit/response");
        paths.setDefaultRequest("/default/request"); paths.setDefaultResponse("/default/response"); props.setPath(paths);
        props.setDefaultToken("module-token"); props.setDefaultHeaders(Map.of("X-Default", "module"));
        var loader = new MockMvcLoader(new Fixtures(), props);
        if (!http) return new MockMvcJournal(MAPPER, loader, MockMvcBuilders.standaloneSetup(new Echo()).build(), props);
        environment.setProperty("service.a", server());
        environment.setProperty("service.b", server());
        return MockMvcJournal.forHttp(MAPPER, loader, props, environment, client);
    }
    private MockMvcJournal bound(MockMvcJournal journal, boolean http) {
        return http ? journal.bind(ServiceContract.builder().baseUrlFromProperty("service.a").build()) : journal;
    }
    private MockMvcBuilder<Payload,Payload> echo(MockMvcJournal journal) {
        return journal.ping(ECHO).withPathParameters(PathParams.create().add("id", "42"))
                .withQueryParameters(QueryParams.create().add("q", List.of("one", "two")).add("q", new String[]{"three"}));
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void sameDefaultsMutatorsAndExplicitOverridesWorkAcrossTransports(boolean http) throws Exception {
        var journal = bound(journal(http), http);
        echo(journal).assertDefault(m -> m.mutateRequest(p -> p.text = "Привіт").mutateResponse(p -> p.text = "Привіт"));
        assertThat(received.getLast().authorization()).isEqualTo("module-token");
        assertThat(received.getLast().header()).isEqualTo("module");
        assertThat(received.getLast().cookie()).contains("session=default");
        assertThat(received.getLast().body()).contains("Привіт");
        echo(journal).applyDefault(c -> c.token("applied").header("X-Default", "applied").cookie("session", "applied"))
                .withRequest("body.json", p -> p.text = "explicit").expectResponse("body.json", p -> p.text = "explicit")
                .token(null).header("X-Default", "explicit").cookie("session", "explicit").assertDefault();
        assertThat(received.getLast().authorization()).isNull();
        assertThat(received.getLast().header()).isEqualTo("explicit");
        assertThat(received.getLast().cookie()).contains("session=explicit");
        echo(journal).token(null).header("Authorization", "explicit-auth").assertDefault();
        assertThat(received.getLast().authorization()).isEqualTo("explicit-auth");
        echo(journal).token("explicit-token").assertDefault();
        assertThat(received.getLast().authorization()).isEqualTo("explicit-token");
        assertThatThrownBy(() -> echo(journal).withRequest("body.json", p -> p.text = "mismatch").assertDefault())
                .isInstanceOf(AssertionError.class);
        echo(journal).withRequest("body.json", p -> p.text = "ignored")
                .expectResponse("body.json", "text").assertDefault();
    }

    @Test void interleavedServiceBuildersDoNotShareAddressHeadersOrBody() throws Exception {
        var root = journal(true);
        var a = echo(root.bind(ServiceContract.builder().baseUrlFromProperty("service.a").build()))
                .token("token-a").withRequest("body.json", p -> p.text = "a").expectResponse("body.json", p -> p.text = "a");
        var b = echo(root.bind(ServiceContract.builder().baseUrlFromProperty("service.b").build()))
                .token("token-b").withRequest("body.json", p -> p.text = "b").expectResponse("body.json", p -> p.text = "b");
        b.assertDefault(); a.assertDefault();
        assertThat(received.get(0).port()).isEqualTo(servers.get(1).getAddress().getPort());
        assertThat(received.get(1).port()).isEqualTo(servers.get(0).getAddress().getPort());
        assertThat(received.get(0).authorization()).isEqualTo("token-b");
        assertThat(received.get(1).authorization()).isEqualTo("token-a");
        assertThat(received.get(0).body()).contains("\"b\"");
        assertThat(received.get(1).body()).contains("\"a\"");
    }

    @Test void preservesBasePathAndEncodesPathAndRepeatedQueryComponentsOnce() throws Exception {
        var journal = bound(journal(true), true);
        journal.ping(Endpoint.createContract("/echo/already%20encoded/{id}", HttpMethod.POST, Payload.class, Payload.class))
                .withPathParameters(PathParams.create().add("id", "ю /?+#%"))
                .withQueryParameters(QueryParams.create().add("q", List.of("a b", "a+b", "x&y", "ю", "%20")))
                .withRequest("body.json").expectResponse("body.json").assertAndCreate();
        assertThat(received.getLast().uri()).isEqualTo("/api/echo/already%20encoded/%D1%8E%20%2F%3F%2B%23%25"
                + "?q=a%20b&q=a%2Bb&q=x%26y&q=%D1%8E&q=%2520");
    }

    record Seen(String uri, String authorization, String header, String cookie, String body, int port) { }
    public static class Payload { public String text; }
    static class Fixtures implements JsonLoader {
        private String path;
        public void setBasePath(String path) { this.path = path; }
        public String getFromFile(String file) {
            if (!"body.json".equals(file)) throw new IllegalArgumentException(file);
            return "{\"text\":\"" + (path.contains("default") ? "default" : "explicit") + "\"}";
        }
        public <T> T getFromFile(String file, Class<T> type) {
            try { return MAPPER.readValue(getFromFile(file), type); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        }
    }
    @RestController class Echo {
        @PostMapping(value = "/echo/{id}", produces = "application/json;charset=UTF-8")
        String echo(@RequestBody String body, HttpServletRequest request) {
            var cookies = request.getCookies();
            received.add(new Seen(request.getRequestURI(), request.getHeader("Authorization"), request.getHeader("X-Default"),
                    cookies == null ? "" : cookies[0].getName() + "=" + cookies[0].getValue(), body, 0));
            assertThat(request.getParameterValues("q")).containsExactly("one", "two", "three");
            return body;
        }
    }
}
