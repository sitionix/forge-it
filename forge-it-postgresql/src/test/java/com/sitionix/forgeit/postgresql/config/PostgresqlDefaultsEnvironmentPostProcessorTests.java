package com.sitionix.forgeit.postgresql.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresqlDefaultsEnvironmentPostProcessorTests {

    private final PostgresqlDefaultsEnvironmentPostProcessor postProcessor =
            new PostgresqlDefaultsEnvironmentPostProcessor();

    @Test
    void shouldLoadDefaultPostgreSqlProperties() {
        MockEnvironment environment = new MockEnvironment();

        this.postProcessor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("forge-it.modules.postgresql.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("forge-it.modules.postgresql.mode")).isEqualTo("internal");
        assertThat(environment.getProperty("forge-it.modules.postgresql.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("forge-it.modules.postgresql.port")).isEqualTo("5432");
        assertThat(environment.getProperty("forge-it.modules.postgresql.template")).isEqualTo("postgresql-template");
    }
}
