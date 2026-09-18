package com.sitionix.forgeit.ros.api;

import java.time.Duration;

public interface RosConsumeBuilder {
    RosConsumeBuilder ignoreFields(String... fields);
    RosConsumeBuilder await(Duration duration);
    RosConsumeBuilder waitUntilAsserted(Duration timeout);
    void assertMessage();
    void assertMessage(String fixture);
}
