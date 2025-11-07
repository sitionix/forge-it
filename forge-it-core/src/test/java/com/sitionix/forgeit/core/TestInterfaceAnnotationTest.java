package com.sitionix.forgeit.core;

import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.core.examples.TestInterfaceDefinition;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestInterfaceAnnotationTest {

    @Test
    void forgeItInheritsRequestedSupport() {
        assertThat(WireMockSupport.class.isAssignableFrom(ForgeIT.class)).isTrue();
    }

    @Test
    void wireMockFeatureMethodsAreCallable() {
        TestInterfaceDefinition testInterface = new TestInterfaceDefinition() { };
        assertThat(testInterface.wiremock()).isEqualTo("wiremock");
    }
}
