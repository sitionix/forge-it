package com.sitionix.forgeit.core.examples;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

/**
 * Example of how a library consumer wires WireMock support into their Forge entry point.
 */
public final class WireMockUsageExample {

    /**
     * Consumers can still annotate their own entry points to opt into additional
     * features. The built-in {@link ForgeIT} contract already carries
     * {@link WireMockSupport}, so the helper becomes available on the generated
     * interface as soon as compilation completes.
     */
    @ForgeFeatures(WireMockSupport.class)
    public interface UserForgeTests extends ForgeIT {
    }

    public String callWireMockFromForgeInterface() {
        UserForgeTests forgeTests = new UserForgeTests() { };
        return forgeTests.wiremock();
    }
}
