package com.sitionix.forgeit.core.internal.test;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ForgeItTestRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String GENERATED_FEATURES_PACKAGE = "com.sitionix.forgeit.core.generated";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        final Class<?> testClass = ClassUtils.resolveClassName(
                importingClassMetadata.getClassName(), ClassUtils.getDefaultClassLoader());
        final Class<?> contractType = resolveContractType(testClass);
        resolveFeatures(contractType);

        final RootBeanDefinition initializerDefinition = new RootBeanDefinition(ForgeItTestInitializer.class);
        registry.registerBeanDefinition("forgeItTestInitializer", initializerDefinition);
    }

    private Class<?> resolveContractType(Class<?> testClass) {
        final Class<?> forgeItType = resolveForgeItType();
        final Set<Class<?>> candidates = new LinkedHashSet<>();
        ReflectionUtils.doWithFields(testClass, field -> {
            if (!Modifier.isStatic(field.getModifiers()) && forgeItType.isAssignableFrom(field.getType())) {
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

    private Class<?> resolveForgeItType() {
        try {
            return ClassUtils.forName("com.sitionix.forgeit.core.api.ForgeIT", ClassUtils.getDefaultClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("ForgeIT type not found on classpath", ex);
        }
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
        if (GENERATED_FEATURES_PACKAGE.equals(type.getPackageName())) {
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
                    && parent != FeatureSupport.class
                    && !GENERATED_FEATURES_PACKAGE.equals(parent.getPackageName())
                    && Arrays.asList(parent.getInterfaces()).contains(FeatureSupport.class)) {
                features.add((Class<? extends FeatureSupport>) parent);
            }
            collectFeatures(parent, features, visited);
        }
    }
}
