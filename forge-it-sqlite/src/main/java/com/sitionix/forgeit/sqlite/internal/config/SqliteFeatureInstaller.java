package com.sitionix.forgeit.sqlite.internal.config;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import com.sitionix.forgeit.sqlite.api.SqliteSupport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the SQLite feature infrastructure into the test application context.
 */
public final class SqliteFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<? extends SqliteSupport> featureType() {
        return SqliteSupport.class;
    }

    @Override
    public void install(final FeatureInstallationContext context) {
        final ConfigurableApplicationContext applicationContext = context.applicationContext();
        if (!(applicationContext instanceof final BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("SQLite installer requires a BeanDefinitionRegistry context");
        }
        registry.registerBeanDefinition(RelationalFeatureMarker.class.getName(),
                new RootBeanDefinition(RelationalFeatureMarker.class));
        new AnnotatedBeanDefinitionReader(registry).register(SqliteFeatureConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = "com.sitionix.forgeit.sqlite.internal")
    static class SqliteFeatureConfiguration {
    }
}
