package com.sitionix.forgeit.wiremock.internal.configs;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
@Component
public final class WireMockContainerManager implements InitializingBean, DisposableBean {

    private static final String PROPERTY_SOURCE_NAME = "forgeItWireMock";
    private static final int WIREMOCK_PORT = 8080;
    private static final DockerImageName WIREMOCK_IMAGE = DockerImageName.parse("wiremock/wiremock:3.6.0");

    private final ConfigurableEnvironment environment;
    private final WireMockProperties properties;

    private GenericContainer<?> container;
    private URI baseUrl;
    private WireMock client;

    @Override
    public void afterPropertiesSet() {
        validateEnabled();
        final WireMockProperties.Mode mode = requireMode();
        if (mode == WireMockProperties.Mode.EXTERNAL) {
            initialiseExternalWireMock();
        } else {
            startInternalWireMock();
        }
        publishEnvironment();
    }

    @Override
    public void destroy() {
        cleanupResources();
    }

    public URI getBaseUrl() {
        if (this.baseUrl == null) {
            throw new IllegalStateException("WireMock base URL has not been initialised");
        }
        return this.baseUrl;
    }

    public WireMock getClient() {
        if (this.client == null) {
            throw new IllegalStateException("WireMock client has not been initialised");
        }
        return this.client;
    }

    private void cleanupResources() {
        removeEnvironment();
        if (this.container != null) {
            this.container.stop();
            this.container = null;
        }
        this.baseUrl = null;
        this.client = null;
    }

    private void publishEnvironment() {
        if (this.environment == null) {
            return;
        }
        if (this.baseUrl == null) {
            throw new IllegalStateException("WireMock container not initialised");
        }
        final MutablePropertySources sources = this.environment.getPropertySources();
        final Map<String, Object> props = Map.of(
                "forge-it.wiremock.base-url", this.baseUrl.toString(),
                "forge-it.wiremock.port", this.baseUrl.getPort(),
                "forge-it.wiremock.host", this.baseUrl.getHost()
        );
        final MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, props);
        if (sources.contains(PROPERTY_SOURCE_NAME)) {
            sources.replace(PROPERTY_SOURCE_NAME, propertySource);
        } else {
            sources.addFirst(propertySource);
        }
    }

    private void removeEnvironment() {
        if (this.environment == null) {
            return;
        }
        final MutablePropertySources sources = this.environment.getPropertySources();
        if (sources.contains(PROPERTY_SOURCE_NAME)) {
            sources.remove(PROPERTY_SOURCE_NAME);
        }
    }

    private void validateEnabled() {
        if (this.properties == null || !Boolean.TRUE.equals(this.properties.getEnabled())) {
            throw new IllegalStateException("WireMock module is disabled via forge-it.modules.wiremock.enabled=false");
        }
    }

    private WireMockProperties.Mode requireMode() {
        final WireMockProperties.Mode mode = this.properties.getMode();
        if (mode == null) {
            throw new IllegalStateException("forge-it.modules.wiremock.mode must be configured");
        }
        return mode;
    }

    private void initialiseExternalWireMock() {
        final String host = this.properties.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("forge-it.modules.wiremock.host must be provided for external mode");
        }
        final Integer port = this.properties.getPort();
        if (port == null || port <= 0) {
            throw new IllegalStateException("forge-it.modules.wiremock.port must be provided for external mode");
        }
        this.baseUrl = URI.create("http://" + host + ":" + port);
        this.client = new WireMock(host, port);
    }

    private void startInternalWireMock() {
        try {
            this.container = new GenericContainer<>(WIREMOCK_IMAGE)
                    .withExposedPorts(WIREMOCK_PORT)
                    .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(60)));
            this.container.start();
            final String host = this.container.getHost();
            final Integer mappedPort = this.container.getMappedPort(WIREMOCK_PORT);
            this.baseUrl = URI.create("http://" + host + ":" + mappedPort);
            this.client = new WireMock(host, mappedPort);
        } catch (RuntimeException ex) {
            cleanupResources();
            throw new IllegalStateException("Failed to start WireMock Testcontainer", ex);
        }
    }
}
