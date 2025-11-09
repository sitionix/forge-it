package com.sitionix.forgeit.wiremock.testing;

import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.wiremock.api.WireMockTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@IntegrationTest(classes = WireMockIntegrationTest.TestApplication.class)
class WireMockIntegrationTest {

    @Autowired
    private UserInterface tools;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void shouldStubWireMockAndExposeAdminEndpoint() {
        final WireMockTool tool = this.tools.wiremock();

        tool.stubFor(get(urlEqualTo("/greeting"))
                .willReturn(aResponse().withStatus(200).withBody("hello")));

        final HttpResponse<String> response = executeGet(tool.baseUrl().resolve("/greeting"));
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello", response.body());

        final String adminPayload = tool.fetchAdminMappings();
        Assertions.assertTrue(adminPayload.contains("/greeting"));
    }

    private HttpResponse<String> executeGet(URI uri) {
        final HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        try {
            return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while calling WireMock stub", ex);
        } catch (IOException ex) {
            throw new AssertionError("Failed to call WireMock stub", ex);
        }
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
