package com.sitionix.forgeit.wiremock.config;

import com.sitionix.forgeit.testing.MockExtension;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class WiremockDefaultsEnvironmentPostProcessorTests extends MockExtension {

    private final WireMockDefaultsEnvironmentPostProcessor postProcessor = new WireMockDefaultsEnvironmentPostProcessor();

    @Test
    void shouldLoadDefaultWireMockProperties() {
        MockEnvironment environment = new MockEnvironment();

        this.postProcessor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("forge-it.modules.wiremock.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("forge-it.modules.wiremock.mode")).isEqualTo("internal");
        assertThat(environment.getProperty("forge-it.modules.wiremock.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("forge-it.modules.wiremock.port")).isEqualTo("8089");
    }
}
