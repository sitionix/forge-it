package com.sitionix.forgeit.wiremock.api;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

/**
 * Runtime handle exposed to integration tests for interacting with the
 * WireMock infrastructure provisioned by ForgeIT.
 */
public final class WireMockTool {

    private final URI baseUrl;
    private final WireMock client;
    private final HttpClient httpClient;

    public WireMockTool(URI baseUrl, WireMock client) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.client = Objects.requireNonNull(client, "client");
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Returns the base HTTP URL of the WireMock server running inside the
     * managed Testcontainer.
     */
    public URI baseUrl() {
        return this.baseUrl;
    }

    /**
     * Provides access to the low-level WireMock client for advanced
     * interactions.
     */
    public WireMock client() {
        return this.client;
    }

    /**
     * Registers a stub mapping with the underlying WireMock instance.
     */
    public void stubFor(MappingBuilder mappingBuilder) {
        this.client.register(Objects.requireNonNull(mappingBuilder, "mappingBuilder"));
    }

    /**
     * Performs a GET request against the WireMock admin endpoint to verify that
     * the managed container is reachable. Returns the JSON payload describing
     * the registered stub mappings.
     */
    public String fetchAdminMappings() {
        final HttpRequest request = HttpRequest.newBuilder(this.baseUrl.resolve("/__admin/mappings"))
                .GET()
                .build();
        try {
            final HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("WireMock admin endpoint returned status " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling WireMock admin endpoint", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to call WireMock admin endpoint", ex);
        }
    }

}
