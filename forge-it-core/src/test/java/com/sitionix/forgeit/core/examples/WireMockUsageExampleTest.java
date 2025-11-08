package com.sitionix.forgeit.core.examples;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WireMockUsageExampleTest {

    private final WireMockUsageExample example = new WireMockUsageExample();

    @Test
    void exposesWireMockFeatureThroughForgeInterface() {
        assertThat(example.callWireMockFromForgeInterface()).isEqualTo("wiremock");
    }
}
