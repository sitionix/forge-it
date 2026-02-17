package com.sitionix.forgeit.mongodb.internal.config;

import org.bson.UuidRepresentation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MongoDataSourceConfigurationTests {

    private final MongoDataSourceConfiguration configuration = new MongoDataSourceConfiguration();

    @Test
    void givenForgeItUuidRepresentationProperty_whenResolveConnectionDetails_thenUseOverrideValue() {
        // given
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("forge-it.mongodb.connection.uri", "mongodb://forge-host:27017/forge-db");
        environment.setProperty("forge-it.mongodb.connection.uuid-representation", "java_legacy");

        final MongoProperties properties = new MongoProperties();
        final MongoProperties.Connection connection = new MongoProperties.Connection();
        connection.setUri("mongodb://module-host:27017/module-db");
        connection.setUuidRepresentation("python_legacy");
        properties.setConnection(connection);

        // when
        final MongoDataSourceConfiguration.ConnectionDetails details =
                this.configuration.resolveConnectionDetails(environment, properties);

        // then
        assertThat(details.uri()).isEqualTo("mongodb://forge-host:27017/forge-db");
        assertThat(details.uuidRepresentation()).isEqualTo(UuidRepresentation.JAVA_LEGACY);
    }

    @Test
    void givenModuleUuidRepresentation_whenResolveConnectionDetails_thenUseModuleValue() {
        // given
        final MockEnvironment environment = new MockEnvironment();

        final MongoProperties properties = new MongoProperties();
        final MongoProperties.Connection connection = new MongoProperties.Connection();
        connection.setUri("mongodb://module-host:27017/module-db");
        connection.setUuidRepresentation("c_sharp_legacy");
        properties.setConnection(connection);

        // when
        final MongoDataSourceConfiguration.ConnectionDetails details =
                this.configuration.resolveConnectionDetails(environment, properties);

        // then
        assertThat(details.uuidRepresentation()).isEqualTo(UuidRepresentation.C_SHARP_LEGACY);
    }

    @Test
    void givenUuidRepresentationMissing_whenResolveConnectionDetails_thenUseStandardByDefault() {
        // given
        final MockEnvironment environment = new MockEnvironment();

        final MongoProperties properties = new MongoProperties();
        final MongoProperties.Connection connection = new MongoProperties.Connection();
        connection.setUri("mongodb://module-host:27017/module-db");
        connection.setUuidRepresentation(null);
        properties.setConnection(connection);

        // when
        final MongoDataSourceConfiguration.ConnectionDetails details =
                this.configuration.resolveConnectionDetails(environment, properties);

        // then
        assertThat(details.uuidRepresentation()).isEqualTo(UuidRepresentation.STANDARD);
    }

    @Test
    void givenUnsupportedUuidRepresentation_whenResolveConnectionDetails_thenThrowIllegalStateException() {
        // given
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("forge-it.mongodb.connection.uuid-representation", "wrong_value");

        final MongoProperties properties = new MongoProperties();
        final MongoProperties.Connection connection = new MongoProperties.Connection();
        connection.setUri("mongodb://module-host:27017/module-db");
        properties.setConnection(connection);

        // when & then
        assertThatThrownBy(() -> this.configuration.resolveConnectionDetails(environment, properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MongoDB UUID representation must be one of");
    }
}
