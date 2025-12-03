package com.sitionix.forgeit.postgresql.internal.config;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.postgresql.api.PostgreSqlSupport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the PostgreSQL feature infrastructure into the test application context.
 */
public final class PostgreSqlFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<? extends PostgreSqlSupport> featureType() {
        return PostgreSqlSupport.class;
    }

    @Override
    public void install(FeatureInstallationContext context) {
        final ConfigurableApplicationContext applicationContext = context.applicationContext();
        if (!(applicationContext instanceof BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("PostgreSQL installer requires a BeanDefinitionRegistry context");
        }
        new AnnotatedBeanDefinitionReader(registry).register(PostgreSqlFeatureConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = "com.sitionix.forgeit.postgresql.internal")
    static class PostgreSqlFeatureConfiguration {
    }
}
