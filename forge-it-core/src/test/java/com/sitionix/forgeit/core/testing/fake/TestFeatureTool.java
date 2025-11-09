package com.sitionix.forgeit.core.testing.fake;

/**
 * Lightweight tool exposed by the fake feature used in core integration tests.
 * Maintains a counter so assertions can verify that the same instance is wired
 * through the proxy and bridge indirection.
 */
public final class TestFeatureTool {

    private int invocationCount;

    public String ping(String value) {
        this.invocationCount++;
        return "pong:" + value;
    }

    public int invocationCount() {
        return this.invocationCount;
    }
}
