package com.sitionix.forgeit.wiremock.internal.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import com.sitionix.forgeit.wiremock.internal.WireMockProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class ResponseMappingResourcesIntegrationTest {

    private static final String APPLICATION_CLASSES_PATH = "../forge-it-application/target/classes";

    @Test
    void shouldLoadResponseMappingUsingApplicationResourcesLoader() throws Exception {
        final ResourcesLoader resourcesLoader = loadApplicationResourcesLoader();
        final WireMockProperties properties = loadDefaultWireMockProperties();

        final ResponseMappingResources responseMappingResources = new ResponseMappingResources(resourcesLoader, properties);

        final String response = responseMappingResources.getFromFile("sample-response.json");

        Assertions.assertThat(response)
                .contains("\"message\": \"sample response\"")
                .contains("\"status\": 200");
    }

    private static ResourcesLoader loadApplicationResourcesLoader() throws Exception {
        final Path classesDirectory = Path.of(APPLICATION_CLASSES_PATH).toAbsolutePath();
        if (!Files.exists(classesDirectory)) {
            throw new IllegalStateException("Application module classes are missing. Build forge-it-application before running tests.");
        }

        final URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDirectory.toUri().toURL()}, ResponseMappingResourcesIntegrationTest.class.getClassLoader());
        final Class<?> implClass = Class.forName("com.sitionix.forgeit.application.loader.ResourcesLoaderImpl", true, classLoader);
        final Object instance = implClass.getConstructor().newInstance();

        final Field objectMapper = implClass.getDeclaredField("objectMapper");
        objectMapper.setAccessible(true);
        objectMapper.set(instance, new ObjectMapper());

        final Method setResourcePath = implClass.getMethod("setResourcePath", String.class);
        setResourcePath.invoke(instance, "/%s");

        return (ResourcesLoader) ResourcesLoader.class.cast(instance);
    }

    private static WireMockProperties loadDefaultWireMockProperties() throws IOException {
        final Resource defaults = new ClassPathResource("forge-it-wiremock-default.yml");
        final List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load("wiremock-defaults", defaults);

        final MutablePropertySources sources = new MutablePropertySources();
        propertySources.forEach(sources::addLast);

        try {
            return new Binder(ConfigurationPropertySources.from(sources))
                    .bind(WireMockProperties.PROPERTY_PREFIX, Bindable.of(WireMockProperties.class))
                    .orElseThrow(() -> new IllegalStateException("WireMock defaults are missing"));
        } catch (BindException ex) {
            throw new IllegalStateException("Failed to bind WireMock default properties", ex);
        }
    }
}
