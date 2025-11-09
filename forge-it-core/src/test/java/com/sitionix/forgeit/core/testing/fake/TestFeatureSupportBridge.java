package com.sitionix.forgeit.core.testing.fake;

import java.util.Objects;

/**
 * Simple bridge used in tests to simulate the delegation pattern implemented by
 * real ForgeIT features. The default methods on the feature interface delegate
 * here so tests can verify the proxy wiring without depending on external
 * modules.
 */
public final class TestFeatureSupportBridge {

    private static volatile TestFeatureTool delegate;
    private static volatile boolean shutdown;

    private TestFeatureSupportBridge() {
    }

    public static TestFeatureTool tool() {
        final TestFeatureTool current = delegate;
        if (current != null) {
            return current;
        }
        if (shutdown) {
            throw new IllegalStateException("Test feature has been shut down");
        }
        throw new IllegalStateException("Test feature has not been initialised");
    }

    public static void register(TestFeatureTool tool) {
        delegate = Objects.requireNonNull(tool, "tool");
        shutdown = false;
    }

    public static void clear() {
        delegate = null;
        shutdown = true;
    }
}
