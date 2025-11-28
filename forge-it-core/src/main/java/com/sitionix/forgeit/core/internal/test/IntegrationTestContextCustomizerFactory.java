package com.sitionix.forgeit.core.internal.test;

import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.core.internal.feature.ForgeFeaturesResolver;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.util.ReflectionUtils;

public final class IntegrationTestContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        final Optional<Class<?>> contractType = findContractType(testClass);
        if (contractType.isEmpty()) {
            return null;
        }
        final List<Class<? extends FeatureSupport>> features = ForgeFeaturesResolver.resolveFeatures(contractType.get());
        return new ForgeIntegrationTestContextCustomizer(contractType.get(), features);
    }

    private Optional<Class<?>> findContractType(Class<?> testClass) {
        final Set<Class<?>> candidates = new LinkedHashSet<>();
        ReflectionUtils.doWithFields(testClass, field -> {
            if (!Modifier.isStatic(field.getModifiers()) && ForgeIT.class.isAssignableFrom(field.getType())) {
                candidates.add(field.getType());
            }
        });
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException("Multiple ForgeIT contracts detected on test class " + testClass.getName());
        }
        final Class<?> contract = candidates.iterator().next();
        if (!contract.isInterface()) {
            throw new IllegalStateException("ForgeIT contracts must be interfaces: " + contract.getName());
        }
        return Optional.of(contract);
    }
}
