package com.sitionix.forgeit.consumer.e2e;

import com.sitionix.forgeit.consumer.auth.endpoint.MockMvcEndpoint;
import com.sitionix.forgeit.core.test.E2E;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

@E2E(properties = "consumer.explicit=override")
class HttpE2eSelfTest {
    @Autowired private E2eSupport forgeIt;
    @Autowired private ApplicationContext context;
    private static final AtomicInteger calls = new AtomicInteger();
    private static HttpServer server;

    @DynamicPropertySource
    static void serverProperties(DynamicPropertyRegistry properties) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/auth/login", exchange -> {
            calls.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            try (var resource = HttpE2eSelfTest.class.getClassLoader().getResourceAsStream(
                    "forge-it/mockmvc/response/loginResponse.json")) {
                byte[] body = resource.readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } finally { exchange.close(); }
        });
        server.start();
        properties.add("consumer.http.auth-base-url", () -> "http://127.0.0.1:" + server.getAddress().getPort() + "/api");
    }
    @AfterAll static void stop() { if (server != null) server.stop(0); }

    @Test void reusesConsumerEndpointAndFixtures() {
        forgeIt.mockMvc(ServiceContracts.AUTH).ping(MockMvcEndpoint.loginDefault()).assertDefault();
        forgeIt.mockMvc(ServiceContracts.AUTH).ping(MockMvcEndpoint.login())
                .withRequest("loginRequest.json").expectResponse("loginResponse.json")
                .expectStatus(HttpStatus.OK).assertAndCreate();
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test void bootstrapsOnlyTestInfrastructure() {
        assertThat(forgeIt.getClass().getSimpleName()).isEqualTo("E2eSupportImpl");
        assertThat(context.getBeansOfType(org.springframework.test.web.servlet.MockMvc.class)).isEmpty();
        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
        assertThat(context.getBeansOfType(com.sitionix.forgeit.consumer.ForgeItConsumerApplication.class)).isEmpty();
        assertThat(context.getEnvironment().getProperty("consumer.explicit")).isEqualTo("override");
        for (String name : context.getBeanDefinitionNames()) {
            assertThat(name.toLowerCase()).doesNotContain("container", "dbcleaner", "wiremock", "entitymanager");
        }
        assertThatThrownBy(() -> forgeIt.mockMvc()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ServiceContract");
    }
}
