package com.sitionix.forgeit.kafka.internal.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public final class KafkaContainerManager implements InitializingBean, DisposableBean {

    private static final String PROPERTY_SOURCE_NAME = "forgeItKafka";
    private static final String DEFAULT_IMAGE = "confluentinc/cp-kafka:7.6.1";

    private final ConfigurableEnvironment environment;
    private final KafkaProperties properties;

    private KafkaContainer container;
    private String bootstrapServers;

    @Override
    public void afterPropertiesSet() {
        if (!this.isEnabled()) {
            return;
        }
        final KafkaProperties.Mode mode = this.requireMode();
        if (mode == KafkaProperties.Mode.EXTERNAL) {
            this.initialiseExternal();
        } else {
            this.startInternal();
        }
        this.publishEnvironment();
    }

    @Override
    public void destroy() {
        this.cleanupResources();
    }

    private void cleanupResources() {
        this.removeEnvironment();
        if (this.container != null) {
            this.container.stop();
            this.container = null;
        }
        this.bootstrapServers = null;
    }

    private void publishEnvironment() {
        if (this.environment == null) {
            return;
        }
        if (this.bootstrapServers == null) {
            throw new IllegalStateException("Kafka bootstrap servers are not initialised");
        }
        final MutablePropertySources sources = this.environment.getPropertySources();
        final KafkaProperties.Consumer consumer = this.properties.getConsumer();
        final Map<String, Object> props = Map.of(
                "forge-it.modules.kafka.bootstrap-servers", this.bootstrapServers,
                "spring.kafka.bootstrap-servers", this.bootstrapServers,
                "spring.kafka.consumer.group-id", consumer == null ? "forge-it-consumer" : consumer.getGroupId()
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

    private boolean isEnabled() {
        return this.properties != null && Boolean.TRUE.equals(this.properties.getEnabled());
    }

    private KafkaProperties.Mode requireMode() {
        final KafkaProperties.Mode mode = this.properties.getMode();
        if (mode == null) {
            throw new IllegalStateException("forge-it.modules.kafka.mode must be configured");
        }
        return mode;
    }

    private KafkaProperties.Container resolveContainer() {
        return this.properties.getContainer();
    }

    private void initialiseExternal() {
        final String configuredServers = this.properties.getBootstrapServers();
        if (configuredServers == null || configuredServers.isBlank()) {
            throw new IllegalStateException("forge-it.modules.kafka.bootstrap-servers must be provided for external mode");
        }
        this.bootstrapServers = configuredServers;
    }

    private void startInternal() {
        final KafkaProperties.Container containerConfig = this.resolveContainer();
        try {
            final String image = Objects.requireNonNullElse(containerConfig == null ? null : containerConfig.getImage(),
                    DEFAULT_IMAGE);
            this.container = new KafkaContainer(DockerImageName.parse(image));
            this.container.start();
            this.bootstrapServers = this.container.getBootstrapServers();
        } catch (final RuntimeException ex) {
            this.cleanupResources();
            throw new IllegalStateException("Failed to start Kafka Testcontainer", ex);
        }
    }
}
