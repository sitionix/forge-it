package com.sitionix.forgeit.core.internal.test;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public final class ForgeItContextTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        this.setContext(testContext);
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        this.setContext(testContext);
    }

    @Override
    public void afterTestClass(final TestContext testContext) {
        FeatureContextHolder.clear();
    }

    private void setContext(final TestContext testContext) {
        final ApplicationContext applicationContext = testContext.getApplicationContext();
        if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
            FeatureContextHolder.setApplicationContext(configurableContext);
        }
    }
}
