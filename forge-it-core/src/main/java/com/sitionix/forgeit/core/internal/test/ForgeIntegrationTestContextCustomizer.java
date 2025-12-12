package com.sitionix.forgeit.core.internal.test;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstallationService;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.List;
import java.util.Objects;

final class ForgeIntegrationTestContextCustomizer implements ContextCustomizer {

    private final Class<?> testClass;
    private final Class<?> contractType;
    private final List<Class<? extends FeatureSupport>> features;

    ForgeIntegrationTestContextCustomizer(Class<?> testClass,
                                          Class<?> contractType,
                                          List<Class<? extends FeatureSupport>> features) {
        this.testClass = testClass;
        this.contractType = contractType;
        this.features = List.copyOf(features);
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        IntegrationTestTransactionAttributeRegistrar.registerIfNecessary(context, this.testClass);
        final FeatureInstallationService installationService =
                new FeatureInstallationService(context.getClassLoader());
        installationService.installFeatures(this.features, new FeatureInstallationContext(context));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ForgeIntegrationTestContextCustomizer that)) {
            return false;
        }
        return this.testClass.equals(that.testClass)
                && this.contractType.equals(that.contractType)
                && this.features.equals(that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.testClass, this.contractType, this.features);
    }
}
