package com.sitionix.forgeit.ros.api;

public interface RosMessaging {
    RosPublishBuilder publish(RosTopicContract contract);
    RosConsumeBuilder consume(RosTopicContract contract);
}
