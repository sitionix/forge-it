package com.sitionix.forgeit.ros.api;

public interface RosPublishBuilder {
    RosPublishBuilder message(String fixture);
    void publish();
    void publishDefault();

    /**
     * Subscribes before publishing and asserts the first feedback message against its default fixture.
     * Uses the configured default consume timeout as one deadline for the complete exchange.
     */
    void publishAndVerify(RosTopicContract feedbackContract);
}
