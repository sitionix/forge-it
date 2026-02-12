package com.sitionix.forgeit.mongodb.internal.config;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Configuration(proxyBeanMethods = false)
public class MongoDataSourceConfiguration {

    private static final String MONGO_PROPERTIES_PREFIX = "forge-it.mongodb.connection";
    private static final String URI_PROPERTY = MONGO_PROPERTIES_PREFIX + ".uri";

    @Bean(name = "forgeItMongoClient", destroyMethod = "close")
    @DependsOn("mongoContainerManager")
    MongoClient mongoClient(final Environment environment,
                            final MongoProperties mongoProperties) {
        final ConnectionDetails details = this.resolveConnectionDetails(environment, mongoProperties);
        return MongoClients.create(details.uri());
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

        return new ConnectionDetails(uri, database);
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

    record ConnectionDetails(String uri, String database) {
    }
}
