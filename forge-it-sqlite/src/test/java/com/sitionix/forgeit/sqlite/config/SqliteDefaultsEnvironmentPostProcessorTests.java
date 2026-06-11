package com.sitionix.forgeit.sqlite.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteDefaultsEnvironmentPostProcessorTests {

    @Test
    void loadsSqliteDefaults() {
        final MockEnvironment environment = new MockEnvironment();

        new SqliteDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("forge-it.modules.sqlite.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("forge-it.modules.sqlite.mode")).isEqualTo("internal");
        assertThat(environment.getProperty("forge-it.modules.sqlite.paths.ddl.path")).isEqualTo("/db/sqlite");
    }
}
