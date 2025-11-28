package com.sitionix.forgeit.wiremock.internal.configs;

import com.sitionix.forgeit.core.internal.feature.FeatureInstallationContext;
import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class WireMockFeatureInstallerTests {

    private final WireMockFeatureInstaller installer = new WireMockFeatureInstaller();

    @Test
    void shouldRegisterWireMockInfrastructureBeans() {
        final GenericApplicationContext applicationContext = new GenericApplicationContext();
        final FeatureInstallationContext context = new FeatureInstallationContext(applicationContext);

        installer.install(context);

        assertThat(applicationContext.containsBeanDefinition(WireMockProperties.BEAN_NAME)).isTrue();
        assertThat(applicationContext.containsBeanDefinition(WireMockContainerManager.BEAN_NAME)).isTrue();
        assertThat(applicationContext.containsBeanDefinition(WireMockJournal.BEAN_NAME)).isTrue();
        assertThat(applicationContext.containsBeanDefinition(WireMockFacade.BEAN_NAME)).isTrue();

        final BeanDefinition facadeDefinition = applicationContext.getBeanDefinition(WireMockFacade.BEAN_NAME);
        final ConstructorArgumentValues.ValueHolder argumentValue =
                facadeDefinition.getConstructorArgumentValues().getGenericArgumentValue(Object.class);

        assertThat(argumentValue).isNotNull();
        assertThat(argumentValue.getValue()).isInstanceOf(RuntimeBeanReference.class);
        assertThat(((RuntimeBeanReference) argumentValue.getValue()).getBeanName())
                .isEqualTo(WireMockJournal.BEAN_NAME);
    }
}
