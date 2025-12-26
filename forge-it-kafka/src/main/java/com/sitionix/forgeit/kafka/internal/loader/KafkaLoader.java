package com.sitionix.forgeit.kafka.internal.loader;

import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.kafka.internal.config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaLoader {

    private final ObjectProvider<JsonLoader> jsonLoaderProvider;
    private final KafkaProperties properties;

    public JsonLoader payloads() {
        final JsonLoader loader = this.jsonLoaderProvider.getIfAvailable();
        if (loader == null) {
            throw new IllegalStateException("JsonLoader bean is not available; ensure forge-it-application is on the classpath");
        }
        final KafkaProperties.Path path = this.properties.getPath();
        if (path == null || path.getPayload() == null) {
            throw new IllegalStateException("Kafka payload path is not configured");
        }
        loader.setBasePath(path.getPayload());
        return loader;
    }

    public JsonLoader expectedPayloads() {
        final JsonLoader loader = this.jsonLoaderProvider.getIfAvailable();
        if (loader == null) {
            throw new IllegalStateException("JsonLoader bean is not available; ensure forge-it-application is on the classpath");
        }
        final KafkaProperties.Path path = this.properties.getPath();
        if (path == null || path.getExpected() == null) {
            throw new IllegalStateException("Kafka expected payload path is not configured");
        }
        loader.setBasePath(path.getExpected());
        return loader;
    }

    public JsonLoader defaultPayloads() {
        final JsonLoader loader = this.jsonLoaderProvider.getIfAvailable();
        if (loader == null) {
            throw new IllegalStateException("JsonLoader bean is not available; ensure forge-it-application is on the classpath");
        }
        final KafkaProperties.Path path = this.properties.getPath();
        if (path == null || path.getDefaultPayload() == null) {
            throw new IllegalStateException("Kafka default payload path is not configured");
        }
        loader.setBasePath(path.getDefaultPayload());
        return loader;
    }

    public JsonLoader defaultExpectedPayloads() {
        final JsonLoader loader = this.jsonLoaderProvider.getIfAvailable();
        if (loader == null) {
            throw new IllegalStateException("JsonLoader bean is not available; ensure forge-it-application is on the classpath");
        }
        final KafkaProperties.Path path = this.properties.getPath();
        if (path == null || path.getDefaultExpected() == null) {
            throw new IllegalStateException("Kafka default expected payload path is not configured");
        }
        loader.setBasePath(path.getDefaultExpected());
        return loader;
    }
}
