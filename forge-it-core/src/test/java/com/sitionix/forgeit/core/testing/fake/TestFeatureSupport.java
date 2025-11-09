package com.sitionix.forgeit.core.testing.fake;

import com.sitionix.forgeit.core.marker.FeatureSupport;

/**
 * Test-only feature contract used to exercise the ForgeIT runtime wiring
 * without introducing module cycles.
 */
public interface TestFeatureSupport extends FeatureSupport {

    default TestFeatureTool testFeature() {
        return TestFeatureSupportBridge.tool();
    }
}
