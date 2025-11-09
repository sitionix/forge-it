package com.sitionix.forgeit.core.internal.feature;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Shared context passed to feature installers to expose the Spring infrastructure
 * that may be customised.
 */
public final class FeatureInstallationContext {

    private final ConfigurableApplicationContext applicationContext;
    private final ConfigurableListableBeanFactory beanFactory;
    private final ConfigurableEnvironment environment;

    public FeatureInstallationContext(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.beanFactory = applicationContext.getBeanFactory();
        this.environment = resolveEnvironment(applicationContext);
    }

    private static ConfigurableEnvironment resolveEnvironment(ConfigurableApplicationContext context) {
        if (context.getEnvironment() instanceof ConfigurableEnvironment configurableEnvironment) {
            return configurableEnvironment;
        }
        throw new IllegalStateException("ForgeIT requires a configurable environment");
    }

    public ConfigurableApplicationContext applicationContext() {
        return this.applicationContext;
    }

    public ConfigurableListableBeanFactory beanFactory() {
        return this.beanFactory;
    }

    public ConfigurableEnvironment environment() {
        return this.environment;
    }
}
