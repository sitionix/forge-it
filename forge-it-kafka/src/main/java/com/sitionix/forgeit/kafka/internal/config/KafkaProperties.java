package com.sitionix.forgeit.kafka.internal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration model for Kafka settings exposed via {@code forge-it.modules.kafka}.
 */
@Data
@ConfigurationProperties(prefix = KafkaProperties.PROPERTY_PREFIX)
@Component
public final class KafkaProperties {

    static final String PROPERTY_PREFIX = "forge-it.modules.kafka";

    private Boolean enabled;
    private Mode mode;
    private String bootstrapServers;
    private Path path;
    private Consumer consumer;
    private Container container;

    public enum Mode {
        INTERNAL,
        EXTERNAL
    }

    @Data
    public static class Path {
        private String payload;
        private String defaultPayload;
        private String expected;
        private String defaultExpected;
    }

    @Data
    public static class Consumer {
        private String groupId;
        private long pollTimeoutMs;
    }

    @Data
    public static class Container {
        private String image;
    }
}
