package com.sitionix.forgeit.core.internal.feature;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ForgeFeaturesResolver {

    private static final String GENERATED_FEATURES_PACKAGE = "com.sitionix.forgeit.core.generated";

    private ForgeFeaturesResolver() {
    }

    public static List<Class<? extends FeatureSupport>> resolveFeatures(Class<?> contractType) {
        final Set<Class<? extends FeatureSupport>> features = new LinkedHashSet<>();
        collectFeatures(contractType, features, new LinkedHashSet<>());
        return List.copyOf(features);
    }

    @SuppressWarnings("unchecked")
    private static void collectFeatures(Class<?> type,
                                        Set<Class<? extends FeatureSupport>> features,
                                        Set<Class<?>> visited) {
        if (!visited.add(type)) {
            return;
        }
        final ForgeFeatures annotation = AnnotatedElementUtils.findMergedAnnotation(type, ForgeFeatures.class);
        if (annotation != null) {
            features.addAll(Arrays.asList(annotation.value()));
        }
        for (Class<?> parent : type.getInterfaces()) {
            if (FeatureSupport.class.isAssignableFrom(parent)
                    && parent != FeatureSupport.class
                    && !GENERATED_FEATURES_PACKAGE.equals(parent.getPackageName())
                    && Arrays.asList(parent.getInterfaces()).contains(FeatureSupport.class)) {
                features.add((Class<? extends FeatureSupport>) parent);
            }
            collectFeatures(parent, features, visited);
        }
    }
}
