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

    private final Class<?> contractType;
    private final List<Class<? extends FeatureSupport>> features;

    ForgeIntegrationTestContextCustomizer(Class<?> contractType,
                                          List<Class<? extends FeatureSupport>> features) {
        this.contractType = contractType;
        this.features = List.copyOf(features);
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
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
        return this.contractType.equals(that.contractType) && this.features.equals(that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.contractType, this.features);
    }
}
