package com.sitionix.forgeit.core.examples;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

/**
 * Example of how a library consumer wires WireMock support into their Forge entry point.
 */
public final class WireMockUsageExample {

    /**
     * The consumer writes a package-private blueprint that extends {@link ForgeIT}
     * and lists the desired features.
     */
    @ForgeFeatures(value = WireMockSupport.class, exposedName = "UserForgeTests")
    interface UserForgeTestsDefinition extends ForgeIT {
    }

    /**
     * After compilation the processor creates a {@code UserForgeTests} interface that
     * inherits both {@link UserForgeTestsDefinition} and {@link WireMockSupport}.
     */
    public String callWireMockFromGeneratedFacade() {
        UserForgeTests forgeTests = new UserForgeTests() { };
        return forgeTests.wiremock();
    }
}
