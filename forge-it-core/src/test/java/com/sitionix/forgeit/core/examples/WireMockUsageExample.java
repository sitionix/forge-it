package com.sitionix.forgeit.core.examples;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.wiremock.api.WireMockSupport;

/**
 * Example of how a library consumer wires WireMock support into their Forge entry point.
 */
public final class WireMockUsageExample {

    /**
     * The consumer annotates their Forge entry point directly. After compilation,
     * {@link ForgeIT} inherits {@link WireMockSupport}, so any extension of ForgeIT
     * gains WireMock helpers automatically.
     */
    @ForgeFeatures(WireMockSupport.class)
    public interface UserForgeTests extends ForgeIT {
    }

    public String callWireMockFromForgeInterface() {
        UserForgeTests forgeTests = new UserForgeTests() { };
        return forgeTests.wiremock();
    }
}
