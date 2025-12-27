package com.sitionix.forgeit.kafka.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaDefaultsEnvironmentPostProcessorTests {

    private final KafkaDefaultsEnvironmentPostProcessor postProcessor =
            new KafkaDefaultsEnvironmentPostProcessor();

    @Test
    void shouldLoadDefaultKafkaProperties() {
        final MockEnvironment environment = new MockEnvironment();

        this.postProcessor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("forge-it.modules.kafka.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("forge-it.modules.kafka.mode")).isEqualTo("internal");
        assertThat(environment.getProperty("forge-it.modules.kafka.bootstrap-servers"))
                .isEqualTo("localhost:9092");
        assertThat(environment.getProperty("forge-it.modules.kafka.path.metadata"))
                .isEqualTo("/kafka/metadata");
        assertThat(environment.getProperty("forge-it.modules.kafka.path.default-metadata"))
                .isEqualTo("/kafka/default/metadata");
    }
}
