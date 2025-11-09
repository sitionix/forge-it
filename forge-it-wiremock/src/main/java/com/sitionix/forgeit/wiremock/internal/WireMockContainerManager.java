package com.sitionix.forgeit.wiremock.internal;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public final class WireMockContainerManager implements InitializingBean, DisposableBean {

    static final String BEAN_NAME = "forgeItWireMockContainerManager";
    private static final String PROPERTY_SOURCE_NAME = "forgeItWireMock";
    private static final int WIREMOCK_PORT = 8080;
    private static final DockerImageName WIREMOCK_IMAGE = DockerImageName.parse("wiremock/wiremock:3.6.0");

    private final ConfigurableEnvironment environment;

    private GenericContainer<?> container;
    private URI baseUrl;
    private WireMock client;

    public WireMockContainerManager(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            this.container = new GenericContainer<>(WIREMOCK_IMAGE)
                    .withExposedPorts(WIREMOCK_PORT)
                    .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(60)));
            this.container.start();
            final String host = this.container.getHost();
            final Integer mappedPort = this.container.getMappedPort(WIREMOCK_PORT);
            this.baseUrl = URI.create("http://" + host + ":" + mappedPort);
            this.client = new WireMock(host, mappedPort);
            publishEnvironment(mappedPort);
        } catch (RuntimeException ex) {
            cleanupResources();
            throw new IllegalStateException("Failed to start WireMock Testcontainer", ex);
        }
    }

    @Override
    public void destroy() {
        cleanupResources();
    }

    public URI baseUrl() {
        if (this.baseUrl == null) {
            throw new IllegalStateException("WireMock container not initialised");
        }
        return this.baseUrl;
    }

    public WireMock client() {
        if (this.client == null) {
            throw new IllegalStateException("WireMock container not initialised");
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

    private void publishEnvironment(int port) {
        if (this.environment == null) {
            return;
        }
        final MutablePropertySources sources = this.environment.getPropertySources();
        final Map<String, Object> properties = Map.of(
                "forgeit.wiremock.base-url", this.baseUrl.toString(),
                "forgeit.wiremock.port", port,
                "forgeit.wiremock.host", this.baseUrl.getHost()
        );
        final MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, properties);
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
}
