package com.sitionix.forgeit.core.internal.feature;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Provides read-only access to the application context for feature support
 * default methods that need to obtain Spring-managed collaborators.
 */
public final class FeatureContextHolder {

    private static volatile ConfigurableApplicationContext context;

    private FeatureContextHolder() {
    }

    public static void setApplicationContext(ConfigurableApplicationContext applicationContext) {
        FeatureContextHolder.context = applicationContext;
    }

    public static void clear() {
        FeatureContextHolder.context = null;
    }

    public static <T> T getBean(Class<T> type) {
        if (FeatureContextHolder.context == null) {
            throw new IllegalStateException("ForgeIT application context is not initialised");
        }
        return FeatureContextHolder.context.getBean(type);
    }
}
