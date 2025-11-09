package com.sitionix.forgeit.wiremock.api;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.net.URI;
import java.util.Objects;

/**
 * Runtime handle exposed to integration tests for interacting with the
 * WireMock infrastructure provisioned by ForgeIT.
 */
public final class WireMockTool {

    private final URI baseUrl;
    private final WireMock client;

    public WireMockTool(URI baseUrl, WireMock client) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.client = Objects.requireNonNull(client, "client");
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
     * Clears all mappings and scenarios from the managed WireMock instance.
     */
    public void resetAll() {
        this.client.resetAll();
    }
}
