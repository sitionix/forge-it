package com.sitionix.forgeit.core;

import com.sitionix.forgeit.wiremock.api.WireMockSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestInterfaceAnnotationTest {

    @Test
    void annotatedInterfaceExtendsRequestedSupport() {
        assertThat(WireMockSupport.class.isAssignableFrom(TestInterface.class)).isTrue();
        assertThat(com.sitionix.forgeit.core.api.ForgeIT.class.isAssignableFrom(TestInterface.class)).isTrue();
    }

    @Test
    void wireMockFeatureMethodsAreCallable() {
        TestInterface testInterface = new TestInterface() { };
        assertThat(testInterface.wiremock()).isEqualTo("wiremock");
    }
}
