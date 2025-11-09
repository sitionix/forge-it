package com.sitionix.forgeit.core.internal.test;

import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IntegrationTestContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        if (!AnnotatedElementUtils.hasAnnotation(testClass, IntegrationTest.class)) {
            return null;
        }
        final Class<?> contractType = resolveContractType(testClass);
        final List<Class<? extends FeatureSupport>> features = List.copyOf(resolveFeatures(contractType));
        return new ForgeIntegrationTestContextCustomizer(contractType, features);
    }

    private Class<?> resolveContractType(Class<?> testClass) {
        final Set<Class<?>> candidates = new LinkedHashSet<>();
        ReflectionUtils.doWithFields(testClass, field -> {
            if (!Modifier.isStatic(field.getModifiers()) && ForgeIT.class.isAssignableFrom(field.getType())) {
                candidates.add(field.getType());
            }
        });
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No ForgeIT contract found on test class " + testClass.getName());
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException("Multiple ForgeIT contracts detected on test class " + testClass.getName());
        }
        final Class<?> contract = candidates.iterator().next();
        if (!contract.isInterface()) {
            throw new IllegalStateException("ForgeIT contracts must be interfaces: " + contract.getName());
        }
        return contract;
    }

    @SuppressWarnings("unchecked")
    private Set<Class<? extends FeatureSupport>> resolveFeatures(Class<?> contractType) {
        final Set<Class<? extends FeatureSupport>> features = new LinkedHashSet<>();
        collectFeatures(contractType, features, new LinkedHashSet<>());
        return features;
    }

    private void collectFeatures(Class<?> type,
                                 Set<Class<? extends FeatureSupport>> features,
                                 Set<Class<?>> visited) {
        if (!visited.add(type)) {
            return;
        }
        final ForgeFeatures annotation = AnnotatedElementUtils.findMergedAnnotation(type, ForgeFeatures.class);
        if (annotation != null) {
            for (Class<? extends FeatureSupport> feature : annotation.value()) {
                features.add(feature);
            }
        }
        for (Class<?> parent : type.getInterfaces()) {
            if (FeatureSupport.class.isAssignableFrom(parent)
                    && Arrays.asList(parent.getInterfaces()).contains(FeatureSupport.class)) {
                features.add((Class<? extends FeatureSupport>) parent);
            }
            collectFeatures(parent, features, visited);
        }
    }
}
