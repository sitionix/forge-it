package com.sitionix.forgeit.core.internal.test;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

public final class ForgeItTestInitializer implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
            FeatureContextHolder.setApplicationContext(configurableContext);
        }
    }
}
