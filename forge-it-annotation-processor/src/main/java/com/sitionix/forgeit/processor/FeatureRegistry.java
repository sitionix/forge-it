package com.sitionix.forgeit.processor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class FeatureRegistry {
    private static final Map<String, Set<String>> FEATURE_TO_SUPPORT = Map.of(
            "com.sitionix.forgeit.wiremock.api.WireMockSupport",
            Set.of("com.sitionix.forgeit.wiremock.api.WireMockSupport")
    );

    public static Set<String> resolveSupportInterfaces(String featureType) {
        return FEATURE_TO_SUPPORT.getOrDefault(featureType, Collections.singleton(featureType));
    }

    private FeatureRegistry() {}
}
