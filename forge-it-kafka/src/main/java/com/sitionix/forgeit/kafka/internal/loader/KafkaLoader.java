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
}
