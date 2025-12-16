package com.sitionix.forgeit.postgresql.internal.config;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.postgresql.api.PostgresqlSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Postgresql feature infrastructure into the test application context.
 */
public final class PostgresqlFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<? extends PostgresqlSupport> featureType() {
        return PostgresqlSupport.class;
    }

    @Override
    public void install(final FeatureInstallationContext context) {
        final ConfigurableApplicationContext applicationContext = context.applicationContext();
        if (!(applicationContext instanceof final BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("Postgresql installer requires a BeanDefinitionRegistry context");
        }
        final boolean enabled = Boolean.TRUE.equals(
                applicationContext.getEnvironment()
                        .getProperty(PostgresqlProperties.PROPERTY_PREFIX + ".enabled", Boolean.class));
        if (!enabled) {
            return;
        }
        new AnnotatedBeanDefinitionReader(registry).register(PostgresqlFeatureConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(PostgresqlSupport.class)
    @ComponentScan(basePackages = "com.sitionix.forgeit.postgresql.internal")
    static class PostgresqlFeatureConfiguration {
    }
}
