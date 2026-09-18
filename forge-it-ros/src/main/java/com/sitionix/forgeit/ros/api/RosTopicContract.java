package com.sitionix.forgeit.ros.api;

import org.springframework.core.env.Environment;

/** Immutable, context-independent description of a ROS topic and its fixtures. */
public final class RosTopicContract {
    private final String topic;
    private final String topicProperty;
    private final String messageType;
    private final RosQos qos;
    private final String publishFixture;
    private final String expectedFixture;

    private RosTopicContract(Builder builder) {
        if ((builder.topic == null) == (builder.topicProperty == null)) {
            throw new IllegalArgumentException("Configure exactly one ROS topic or topic property");
        }
        if (builder.topic != null) validateTopic(builder.topic);
        if (builder.topicProperty != null && !builder.topicProperty.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid ROS topic property name");
        }
        if (builder.messageType == null || !builder.messageType.matches("[A-Za-z][A-Za-z0-9_]*/msg/[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("ROS message type must have package/msg/Message form");
        }
        if (builder.qos == null) throw new IllegalArgumentException("ROS QoS is required");
        topic = builder.topic; topicProperty = builder.topicProperty; messageType = builder.messageType;
        qos = builder.qos; publishFixture = builder.publishFixture; expectedFixture = builder.expectedFixture;
    }
    public static Builder builder() { return new Builder(); }
    public String resolveTopic(Environment environment) {
        String resolved;
        try { resolved = topicProperty == null ? topic : environment.getProperty(topicProperty); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("ROS topic property could not be resolved"); }
        validateTopic(resolved);
        return resolved;
    }
    private static void validateTopic(String value) {
        if (value == null || !value.matches("(?:/|~/)?[A-Za-z_][A-Za-z0-9_]*(?:/[A-Za-z_][A-Za-z0-9_]*)*")) {
            throw new IllegalArgumentException("ROS topic must be a resolved, nonblank valid topic name");
        }
    }
    public String topicTemplate() { return topicProperty == null ? topic : "${" + topicProperty + "}"; }
    public String messageType() { return messageType; }
    public RosQos qos() { return qos; }
    public String defaultPublishMessage() { return publishFixture; }
    public String defaultExpectedMessage() { return expectedFixture; }
    public static final class Builder {
        private String topic, topicProperty, messageType, publishFixture, expectedFixture;
        private RosQos qos = RosQos.reliableVolatile(10);
        private Builder() { }
        public Builder topic(String value) { topic = value; return this; }
        public Builder topicFromProperty(String value) { topicProperty = value; return this; }
        public Builder messageType(String value) { messageType = value; return this; }
        public Builder qos(RosQos value) { qos = value; return this; }
        public Builder defaultPublishMessage(String value) { publishFixture = value; return this; }
        public Builder defaultExpectedMessage(String value) { expectedFixture = value; return this; }
        public RosTopicContract build() { return new RosTopicContract(this); }
    }
}
