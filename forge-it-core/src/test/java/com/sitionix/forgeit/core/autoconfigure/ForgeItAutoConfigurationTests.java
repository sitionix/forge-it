package com.sitionix.forgeit.core.autoconfigure;

import com.sitionix.forgeit.core.api.ForgeIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ForgeItAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ForgeItAutoConfiguration.class))
            .withInitializer(context -> AutoConfigurationPackages.register(context.getBeanFactory(),
                    "com.sitionix.forgeit.core.autoconfigure"));

    @Test
    void registersForgeItContractProxy() {
        TestFeatureInstaller.installed.clear();
        this.contextRunner
                .withUserConfiguration(SampleConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(SampleContract.class));
    }

    @Test
    void installsFeaturesForContracts() {
        TestFeatureInstaller.installed.clear();
        this.contextRunner
                .withUserConfiguration(SampleConfiguration.class)
                .run(context -> assertThat(TestFeatureInstaller.installed)
                        .containsExactly(TestFeature.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class SampleConfiguration {
    }

    @com.sitionix.forgeit.core.annotation.ForgeFeatures(TestFeature.class)
    interface SampleContract extends ForgeIT {
    }

    interface TestFeature extends com.sitionix.forgeit.core.marker.FeatureSupport {
    }

    static class TestFeatureInstaller implements com.sitionix.forgeit.core.internal.feature.FeatureInstaller {

        static final List<Class<?>> installed = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public void install(com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext context) {
            installed.add(TestFeature.class);
        }

        @Override
        public Class<? extends com.sitionix.forgeit.core.marker.FeatureSupport> featureType() {
            return TestFeature.class;
        }
    }
}
