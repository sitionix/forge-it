package com.sitionix.forgeit.kafka.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

/**
 * Loads the default Kafka YAML settings when the module is present on the classpath.
 */
public class KafkaDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DEFAULT_RESOURCE = "forge-it-kafka-default.yml";

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment,
                                       final SpringApplication application) {
        this.loadYaml(DEFAULT_RESOURCE, environment);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void loadYaml(final String resourcePath, final ConfigurableEnvironment environment) {
        final Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            return;
        }
        try {
            final List<PropertySource<?>> propertySources = this.loader.load(resourcePath, resource);
            propertySources.forEach(ps -> environment.getPropertySources().addLast(ps));
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to load Kafka default file " + resourcePath, ex);
        }
    }
}
