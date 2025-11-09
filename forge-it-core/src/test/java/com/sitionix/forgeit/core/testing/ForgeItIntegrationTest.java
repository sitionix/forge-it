package com.sitionix.forgeit.core.testing;

import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.wiremock.api.WireMockTool;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@IntegrationTest
class ForgeItIntegrationTest {

    @Autowired
    private UserInterface tools;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void shouldDelegateWireMockFeatureThroughProxy() {
        final WireMockTool wireMock = this.tools.wiremock();

        wireMock.stubFor(get(urlEqualTo("/greeting"))
                .willReturn(aResponse().withStatus(200).withBody("hello")));

        final HttpResponse<String> response = executeGet(wireMock.baseUrl().resolve("/greeting"));
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.body()).isEqualTo("hello");

        final String adminPayload = wireMock.fetchAdminMappings();
        Assertions.assertThat(adminPayload).contains("/greeting");
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
}
