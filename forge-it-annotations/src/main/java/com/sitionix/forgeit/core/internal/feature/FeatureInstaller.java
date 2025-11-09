package com.sitionix.forgeit.core.internal.feature;

import com.sitionix.forgeit.core.marker.FeatureSupport;

/**
 * Runtime hook implemented by feature modules to bootstrap their infrastructure
 * inside a ForgeIT-powered Spring application context.
 */
public interface FeatureInstaller {

    /**
     * @return the feature interface that this installer provisions.
     */
    Class<? extends FeatureSupport> featureType();

    /**
     * Register beans and perform any additional setup required for the feature
     * before the application context is refreshed.
     */
    void install(FeatureInstallationContext context);
}
