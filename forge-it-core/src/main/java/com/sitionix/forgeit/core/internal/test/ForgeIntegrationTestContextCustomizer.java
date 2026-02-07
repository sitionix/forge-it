package com.sitionix.forgeit.core.internal.test;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstallationService;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ForgeIntegrationTestContextCustomizer implements ContextCustomizer {

    private static final String POSTGRESQL_SUPPORT = "com.sitionix.forgeit.postgresql.api.PostgresqlSupport";
    private static final String WIREMOCK_SUPPORT = "com.sitionix.forgeit.wiremock.api.WireMockSupport";
    private static final String MOCKMVC_SUPPORT = "com.sitionix.forgeit.mockmvc.api.MockMvcSupport";
    private static final String KAFKA_SUPPORT = "com.sitionix.forgeit.kafka.api.KafkaSupport";
    private static final String AUTOCONFIG_EXCLUDE_KEY = "spring.autoconfigure.exclude";
    private static final String DATASOURCE_AUTOCONFIG =
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration";
    private static final String PROPERTY_SOURCE_NAME = "forgeItAutoConfig";
    private static final String FEATURE_TOGGLES_SOURCE = "forgeItFeatureToggles";

    private final Class<?> contractType;
    private final List<Class<? extends FeatureSupport>> features;
    private final List<String> testProperties;

    ForgeIntegrationTestContextCustomizer(Class<?> contractType,
                                          List<Class<? extends FeatureSupport>> features,
                                          List<String> testProperties) {
        this.contractType = contractType;
        this.features = List.copyOf(features);
        this.testProperties = List.copyOf(testProperties);
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        applyTestProperties(context);
        applyFeatureToggles(context);
        disableDataSourceAutoConfigurationIfUnused(context);
        final FeatureInstallationService installationService =
                new FeatureInstallationService(context.getClassLoader());
        installationService.installFeatures(this.features, new FeatureInstallationContext(context));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ForgeIntegrationTestContextCustomizer that)) {
            return false;
        }
        return this.contractType.equals(that.contractType)
                && this.features.equals(that.features)
                && this.testProperties.equals(that.testProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.contractType, this.features, this.testProperties);
    }

    private void disableDataSourceAutoConfigurationIfUnused(ConfigurableApplicationContext context) {
        if (hasFeature(POSTGRESQL_SUPPORT)) {
            return;
        }
        final ConfigurableEnvironment environment = context.getEnvironment();
        if (hasDataSourceConfiguration(environment)) {
            return;
        }
        final String existing = environment.getProperty(AUTOCONFIG_EXCLUDE_KEY);
        final Set<String> excludes = new LinkedHashSet<>();
        if (existing != null && !existing.isBlank()) {
            for (String value : existing.split(",")) {
                final String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    excludes.add(trimmed);
                }
            }
        }
        if (!excludes.add(DATASOURCE_AUTOCONFIG)) {
            return;
        }
        final MutablePropertySources sources = environment.getPropertySources();
        final MapPropertySource propertySource = new MapPropertySource(
                PROPERTY_SOURCE_NAME,
                Map.of(AUTOCONFIG_EXCLUDE_KEY, String.join(",", excludes))
        );
        if (sources.contains(PROPERTY_SOURCE_NAME)) {
            sources.replace(PROPERTY_SOURCE_NAME, propertySource);
        } else {
            sources.addFirst(propertySource);
        }
    }

    private void applyFeatureToggles(ConfigurableApplicationContext context) {
        final Map<String, Object> toggles = Map.of(
                "forge-it.modules.postgresql.enabled", hasFeature(POSTGRESQL_SUPPORT),
                "forge-it.modules.wiremock.enabled", hasFeature(WIREMOCK_SUPPORT),
                "forge-it.modules.mock-mvc.enabled", hasFeature(MOCKMVC_SUPPORT),
                "forge-it.modules.kafka.enabled", hasFeature(KAFKA_SUPPORT)
        );
        final MutablePropertySources sources = context.getEnvironment().getPropertySources();
        final MapPropertySource propertySource = new MapPropertySource(FEATURE_TOGGLES_SOURCE, toggles);
        if (sources.contains(FEATURE_TOGGLES_SOURCE)) {
            sources.replace(FEATURE_TOGGLES_SOURCE, propertySource);
        } else {
            sources.addFirst(propertySource);
        }
    }

    private boolean hasFeature(String featureName) {
        return this.features.stream().anyMatch(feature -> featureName.equals(feature.getName()));
    }

    private boolean hasDataSourceConfiguration(ConfigurableEnvironment environment) {
        return hasProperty(environment, "spring.datasource.url")
                || hasProperty(environment, "spring.datasource.jdbc-url")
                || hasProperty(environment, "spring.datasource.driver-class-name")
                || hasProperty(environment, "spring.datasource.driverClassName");
    }

    private boolean hasProperty(ConfigurableEnvironment environment, String key) {
        final String value = environment.getProperty(key);
        return value != null && !value.isBlank();
    }

    private void applyTestProperties(ConfigurableApplicationContext context) {
        if (this.testProperties.isEmpty()) {
            return;
        }
        final Map<String, Object> properties = new LinkedHashMap<>();
        for (final String entry : this.testProperties) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            final int separator = entry.indexOf('=');
            if (separator <= 0) {
                throw new IllegalStateException("Invalid test property format (expected key=value): " + entry);
            }
            final String key = entry.substring(0, separator).trim();
            final String value = entry.substring(separator + 1).trim();
            if (key.isEmpty()) {
                throw new IllegalStateException("Invalid test property key: " + entry);
            }
            properties.put(key, value);
        }
        if (properties.isEmpty()) {
            return;
        }
        final MutablePropertySources sources = context.getEnvironment().getPropertySources();
        final MapPropertySource propertySource = new MapPropertySource("forgeItIntegrationTestProperties", properties);
        if (sources.contains(propertySource.getName())) {
            sources.replace(propertySource.getName(), propertySource);
        } else {
            sources.addFirst(propertySource);
        }
    }
}
