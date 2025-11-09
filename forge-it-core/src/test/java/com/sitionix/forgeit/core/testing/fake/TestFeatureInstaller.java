package com.sitionix.forgeit.core.testing.fake;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import com.sitionix.forgeit.core.marker.FeatureSupport;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Registers the fake feature infrastructure during tests so that the contract
 * proxy can delegate calls through the shared bridge.
 */
public final class TestFeatureInstaller implements FeatureInstaller {

    @Override
    public Class<? extends FeatureSupport> featureType() {
        return TestFeatureSupport.class;
    }

    @Override
    public void install(FeatureInstallationContext context) {
        final ConfigurableListableBeanFactory beanFactory = context.beanFactory();
        if (!beanFactory.containsSingleton(TestFeatureTool.class.getName())) {
            final TestFeatureTool tool = new TestFeatureTool();
            beanFactory.registerSingleton(TestFeatureTool.class.getName(), tool);
            TestFeatureSupportBridge.register(tool);
        }
        context.applicationContext().addApplicationListener(event -> {
            if (event instanceof ContextClosedEvent) {
                TestFeatureSupportBridge.clear();
            }
        });
    }
}
