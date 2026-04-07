package com.sitionix.forgeit.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class CoreDefaultsEnvironmentPostProcessorTest {

    private final CoreDefaultsEnvironmentPostProcessor postProcessor = new CoreDefaultsEnvironmentPostProcessor();

    @Test
    void givenMissingDevJwtSecret_whenPostProcessEnvironment_thenApplyCoreDefaultsAndDevJwtSecret() {
        //given
        final MockEnvironment environment = new MockEnvironment();

        //when
        this.postProcessor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        //then
        assertThat(environment.getProperty("forge-it.core.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("forge-it.resources.expectedBasePath")).isEqualTo("json/expected");
        assertThat(environment.getProperty("FORGE_SECURITY_DEV_JWT_SECRET"))
                .isEqualTo("dev-internal-auth-secret");
        assertThat(environment.getProperty("forge.security.dev.jwt-secret"))
                .isEqualTo("dev-internal-auth-secret");
    }

    @Test
    void givenExplicitDevJwtSecret_whenPostProcessEnvironment_thenKeepExplicitSecret() {
        //given
        final MockEnvironment environment = new MockEnvironment()
                .withProperty("FORGE_SECURITY_DEV_JWT_SECRET", "custom-secret");

        //when
        this.postProcessor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        //then
        assertThat(environment.getProperty("FORGE_SECURITY_DEV_JWT_SECRET")).isEqualTo("custom-secret");
        assertThat(environment.getProperty("forge.security.dev.jwt-secret"))
                .isEqualTo("custom-secret");
    }

    @Test
    void givenEnvironmentPostProcessor_whenGetOrder_thenRunBeforeConfigDataProcessing() {
        //given

        //when
        final int order = this.postProcessor.getOrder();

        //then
        assertThat(order).isLessThan(ConfigDataEnvironmentPostProcessor.ORDER);
    }
}
