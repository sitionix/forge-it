package com.sitionix.forgeit.bundle.config;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.boot.env.YamlPropertySourceLoader;

/**
 * Ensures ForgeIT default YAML resources are available without manual imports.
 */
public class ForgeItDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String[] DEFAULT_YAML_RESOURCES = {
            "forge-it-core-default.yml",
            "forge-it-wiremock-default.yml",
            "forge-it-mockmvc-default.yml"
    };

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String resourcePath : DEFAULT_YAML_RESOURCES) {
            loadYaml(resourcePath, environment);
        }
    }

    private void loadYaml(String resourcePath, ConfigurableEnvironment environment) {
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            return;
        }
        try {
            List<PropertySource<?>> propertySources = loader.load(resourcePath, resource);
            propertySources.forEach(ps -> environment.getPropertySources().addLast(ps));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load ForgeIT default file " + resourcePath, ex);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
