package com.sitionix.forgeit.ros.api;

import java.time.Duration;

public interface RosConsumeBuilder {
    RosConsumeBuilder ignoreFields(String... fields);
    RosConsumeBuilder await(Duration duration);
    RosConsumeBuilder waitUntilAsserted(Duration timeout);
    /** Checks every fresh message for the full window. await() bounds the maximum gap. */
    void assertMessageThroughout(Duration window);
    void assertMessage();
    void assertMessage(String fixture);
}
