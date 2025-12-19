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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ForgeIntegrationTestContextCustomizer implements ContextCustomizer {

    private static final String POSTGRESQL_SUPPORT = "com.sitionix.forgeit.postgresql.api.PostgresqlSupport";
    private static final String AUTOCONFIG_EXCLUDE_KEY = "spring.autoconfigure.exclude";
    private static final String DATASOURCE_AUTOCONFIG =
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration";
    private static final String PROPERTY_SOURCE_NAME = "forgeItAutoConfig";

    private final Class<?> contractType;
    private final List<Class<? extends FeatureSupport>> features;

    ForgeIntegrationTestContextCustomizer(Class<?> contractType,
                                          List<Class<? extends FeatureSupport>> features) {
        this.contractType = contractType;
        this.features = List.copyOf(features);
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
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
        return this.contractType.equals(that.contractType) && this.features.equals(that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.contractType, this.features);
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
}
