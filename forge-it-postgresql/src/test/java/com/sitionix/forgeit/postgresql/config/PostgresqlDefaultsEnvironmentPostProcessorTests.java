package com.sitionix.forgeit.postgresql.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresqlDefaultsEnvironmentPostProcessorTests {

    private final PostgresqlDefaultsEnvironmentPostProcessor postProcessor =
            new PostgresqlDefaultsEnvironmentPostProcessor();

    @Test
    void shouldLoadDefaultPostgresqlProperties() {
        final MockEnvironment environment = new MockEnvironment();

        this.postProcessor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("forge-it.modules.postgresql.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("forge-it.modules.postgresql.mode")).isEqualTo("internal");
        assertThat(environment.getProperty("forge-it.modules.postgresql.connection.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("forge-it.modules.postgresql.connection.port")).isEqualTo("5432");
    }
}
