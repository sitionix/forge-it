package com.sitionix.forgeit.kafka.internal.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public final class KafkaContainerManager implements InitializingBean, SmartLifecycle, DisposableBean {

    private static final String PROPERTY_SOURCE_NAME = "forgeItKafka";
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(120);
    private static final int STARTUP_ATTEMPTS = 3;

    private final ConfigurableEnvironment environment;
    private final KafkaProperties properties;

    private KafkaContainer container;
    private String bootstrapServers;
    private volatile boolean running;

    @Override
    public void afterPropertiesSet() {
        this.start();
    }

    @Override
    public void start() {
        if (this.running) {
            return;
        }
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
        this.running = true;
    }

    @Override
    public void stop() {
        if (!this.running) {
            return;
        }
        this.cleanupResources();
        this.running = false;
    }

    @Override
    public void stop(final Runnable callback) {
        this.stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void destroy() {
        this.stop();
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
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("forge-it.modules.kafka.bootstrap-servers", this.bootstrapServers);
        props.put("spring.kafka.bootstrap-servers", this.bootstrapServers);
        if (this.environment.getProperty("spring.kafka.consumer.key-deserializer") == null) {
            props.put("spring.kafka.consumer.key-deserializer", StringDeserializer.class.getName());
        }
        if (this.environment.getProperty("spring.kafka.consumer.value-deserializer") == null) {
            props.put("spring.kafka.consumer.value-deserializer", ByteArrayDeserializer.class.getName());
        }
        if (this.environment.getProperty("spring.kafka.producer.key-serializer") == null) {
            props.put("spring.kafka.producer.key-serializer", StringSerializer.class.getName());
        }
        if (this.environment.getProperty("spring.kafka.producer.value-serializer") == null) {
            props.put("spring.kafka.producer.value-serializer", StringSerializer.class.getName());
        }
        if (consumer != null) {
            final String groupId = consumer.getGroupId();
            if (groupId != null && !groupId.isBlank()
                    && this.environment.getProperty("spring.kafka.consumer.group-id") == null) {
                props.put("spring.kafka.consumer.group-id", groupId);
            }
            final String autoOffsetReset = consumer.getAutoOffsetReset();
            if (autoOffsetReset != null && !autoOffsetReset.isBlank()
                    && this.environment.getProperty("spring.kafka.consumer.auto-offset-reset") == null) {
                props.put("spring.kafka.consumer.auto-offset-reset", autoOffsetReset);
            }
        }
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

    private void initialiseExternal() {
        final String configuredServers = this.properties.getBootstrapServers();
        if (configuredServers == null || configuredServers.isBlank()) {
            throw new IllegalStateException("forge-it.modules.kafka.bootstrap-servers must be provided for external mode");
        }
        this.bootstrapServers = configuredServers;
    }

    private void startInternal() {
        final KafkaProperties.Container containerConfig = this.properties.getContainer();
        if (containerConfig == null || containerConfig.getImage() == null || containerConfig.getImage().isBlank()) {
            throw new IllegalStateException("forge-it.modules.kafka.container.image must be configured for internal mode");
        }
        try {
            this.container = new KafkaContainer(DockerImageName.parse(containerConfig.getImage()));
            this.container.withStartupAttempts(STARTUP_ATTEMPTS);
            this.container.withStartupTimeout(STARTUP_TIMEOUT);
            this.container.start();
            this.bootstrapServers = this.container.getBootstrapServers();
        } catch (final RuntimeException ex) {
            this.cleanupResources();
            throw new IllegalStateException("Failed to start Kafka Testcontainer", ex);
        }
    }
}
