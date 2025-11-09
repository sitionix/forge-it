package com.sitionix.forgeit.core.testing;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.sitionix.forgeit.core.test.IntegrationTest;
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
        final String baseUrlValue = this.tools.wiremock();
        final URI baseUrl = URI.create(baseUrlValue);

        final WireMock client = new WireMock(baseUrl.getHost(), baseUrl.getPort());
        client.register(get(urlEqualTo("/greeting"))
                .willReturn(aResponse().withStatus(200).withBody("hello")));

        final HttpResponse<String> response = executeGet(baseUrl.resolve("/greeting"));
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.body()).isEqualTo("hello");

        final HttpResponse<String> adminResponse = executeGet(baseUrl.resolve("/__admin/mappings"));
        Assertions.assertThat(adminResponse.statusCode()).isEqualTo(200);
        Assertions.assertThat(adminResponse.body()).contains("/greeting");
    }

    private HttpResponse<String> executeGet(URI uri) {
        final HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        try {
            return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while calling WireMock", ex);
        } catch (IOException ex) {
            throw new AssertionError("Failed to call WireMock", ex);
        }
    }
}
