package com.sitionix.forgeit.mongodb.internal.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
public class MongoDataSourceConfiguration {

    private static final String MONGO_PROPERTIES_PREFIX = "forge-it.mongodb.connection";
    private static final String URI_PROPERTY = MONGO_PROPERTIES_PREFIX + ".uri";
    private static final String UUID_REPRESENTATION_PROPERTY = MONGO_PROPERTIES_PREFIX + ".uuid-representation";

    @Bean(name = "forgeItMongoClient", destroyMethod = "close")
    @DependsOn("mongoContainerManager")
    MongoClient mongoClient(final Environment environment,
                            final MongoProperties mongoProperties) {
        final ConnectionDetails details = this.resolveConnectionDetails(environment, mongoProperties);
        final MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(details.uri()))
                .uuidRepresentation(details.uuidRepresentation())
                .build();
        return MongoClients.create(clientSettings);
    }

    @Bean
    @Primary
    @DependsOn("mongoContainerManager")
    MongoTemplate mongoTemplate(@Qualifier("forgeItMongoClient") final MongoClient mongoClient,
                                final Environment environment,
                                final MongoProperties mongoProperties) {
        final ConnectionDetails details = this.resolveConnectionDetails(environment, mongoProperties);
        return new MongoTemplate(mongoClient, details.database());
    }

    ConnectionDetails resolveConnectionDetails(final Environment environment,
                                               final MongoProperties mongoProperties) {
        final MongoProperties.Connection connection = Objects.requireNonNull(mongoProperties.getConnection(),
                "forge-it.modules.mongodb.connection must be configured");

        final String uri = this.requireText(this.resolveUri(environment, connection), "URI", URI_PROPERTY);
        final String database = this.resolveDatabase(uri, connection);
        final UuidRepresentation uuidRepresentation = this.resolveUuidRepresentation(environment, connection);

        return new ConnectionDetails(uri, database, uuidRepresentation);
    }

    private String resolveUri(final Environment environment, final MongoProperties.Connection connection) {
        final String configuredUri = environment.getProperty(URI_PROPERTY);
        if (StringUtils.hasText(configuredUri)) {
            return configuredUri;
        }
        if (StringUtils.hasText(connection.getUri())) {
            return connection.getUri();
        }

        final String host = Objects.requireNonNullElse(connection.getHost(), "localhost");
        final Integer port = Objects.requireNonNullElse(connection.getPort(), 27017);
        final String database = Objects.requireNonNullElse(connection.getDatabase(), "forge-it");

        return "mongodb://" + host + ":" + port + "/" + database;
    }

    private UuidRepresentation resolveUuidRepresentation(final Environment environment,
                                                         final MongoProperties.Connection connection) {
        final String configuredUuidRepresentation = this.resolveWithDefault(environment, UUID_REPRESENTATION_PROPERTY,
                connection.getUuidRepresentation());
        final String normalizedUuidRepresentation = this.normalizeUuidRepresentation(configuredUuidRepresentation);
        try {
            return UuidRepresentation.valueOf(normalizedUuidRepresentation);
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException("MongoDB UUID representation must be one of "
                    + "standard, java_legacy, c_sharp_legacy, python_legacy, unspecified via "
                    + UUID_REPRESENTATION_PROPERTY + " or forge-it.modules.mongodb.connection.uuid-representation",
                    exception);
        }
    }

    private String resolveDatabase(final String uri, final MongoProperties.Connection connection) {
        final String configuredDatabase = connection.getDatabase();
        if (StringUtils.hasText(configuredDatabase)) {
            return configuredDatabase;
        }

        final ConnectionString connectionString = new ConnectionString(uri);
        if (StringUtils.hasText(connectionString.getDatabase())) {
            return connectionString.getDatabase();
        }
        return "forge-it";
    }

    private String requireText(final String value, final String label, final String propertyKey) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        throw new IllegalStateException("MongoDB " + label + " must be configured via " + propertyKey
                + " or forge-it.modules.mongodb.connection");
    }

    private String resolveWithDefault(final Environment environment, final String key, final String defaultValue) {
        final String value = environment.getProperty(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String normalizeUuidRepresentation(final String rawUuidRepresentation) {
        final String uuidRepresentation = StringUtils.hasText(rawUuidRepresentation)
                ? rawUuidRepresentation
                : UuidRepresentation.STANDARD.name();
        return uuidRepresentation.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    record ConnectionDetails(String uri, String database, UuidRepresentation uuidRepresentation) {
    }
}
