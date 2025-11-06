package com.sitionix.forgeit.core.feature;

import java.util.Set;

public interface FeatureProvider {

    ForgeFeature key();

    Set<Class<?>> supportInterfaces();

    Object implementationFor(Class<?> supportInterface, ForgeContext context);
}
