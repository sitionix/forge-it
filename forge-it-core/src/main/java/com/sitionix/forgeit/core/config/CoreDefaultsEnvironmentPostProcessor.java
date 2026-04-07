package com.sitionix.forgeit.core.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads ForgeIT core defaults and ensures a deterministic dev JWT secret for integration tests.
 */
public class CoreDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DEFAULT_RESOURCE = "forge-it-core-default.yml";
    private static final String DEFAULTS_SOURCE = "forgeItCoreDefaults";
    private static final String DEV_JWT_SECRET_ENV = "FORGE_SECURITY_DEV_JWT_SECRET";
    private static final String DEV_JWT_SECRET_PROPERTY = "forge.security.dev.jwt-secret";
    private static final String DEFAULT_DEV_JWT_SECRET = "dev-internal-auth-secret";

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        this.loadYaml(DEFAULT_RESOURCE, environment);
        this.applyDefaultDevJwtSecret(environment);
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
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
            throw new IllegalStateException("Failed to load ForgeIT core default file " + resourcePath, ex);
        }
    }

    private void applyDefaultDevJwtSecret(final ConfigurableEnvironment environment) {
        final String configuredSecret = this.resolveConfiguredSecret(environment);
        final Map<String, Object> properties = new LinkedHashMap<>();
        if (!this.hasTextProperty(environment, DEV_JWT_SECRET_ENV)) {
            properties.put(DEV_JWT_SECRET_ENV, configuredSecret);
        }
        if (!this.hasTextProperty(environment, DEV_JWT_SECRET_PROPERTY)) {
            properties.put(DEV_JWT_SECRET_PROPERTY, configuredSecret);
        }
        if (properties.isEmpty()) {
            return;
        }
        final MutablePropertySources sources = environment.getPropertySources();
        final MapPropertySource propertySource = new MapPropertySource(
                DEFAULTS_SOURCE,
                properties
        );
        if (sources.contains(DEFAULTS_SOURCE)) {
            sources.replace(DEFAULTS_SOURCE, propertySource);
        } else {
            sources.addLast(propertySource);
        }
    }

    private String resolveConfiguredSecret(final ConfigurableEnvironment environment) {
        if (this.hasTextProperty(environment, DEV_JWT_SECRET_PROPERTY)) {
            return environment.getProperty(DEV_JWT_SECRET_PROPERTY);
        }
        if (this.hasTextProperty(environment, DEV_JWT_SECRET_ENV)) {
            return environment.getProperty(DEV_JWT_SECRET_ENV);
        }
        return DEFAULT_DEV_JWT_SECRET;
    }

    private boolean hasTextProperty(final ConfigurableEnvironment environment, final String key) {
        final String value = environment.getProperty(key);
        return value != null && !value.isBlank();
    }
}
