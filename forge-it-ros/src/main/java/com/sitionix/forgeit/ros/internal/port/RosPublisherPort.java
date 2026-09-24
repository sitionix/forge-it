package com.sitionix.forgeit.ros.internal.port;

import com.fasterxml.jackson.databind.JsonNode;
import com.sitionix.forgeit.ros.api.RosQos;

import java.time.Duration;

public interface RosPublisherPort {

    void publish(String topic, String messageType, RosQos qos, JsonNode message, Duration timeout);
    void publishPeriodically(String topic, String messageType, RosQos qos, JsonNode message,
                             Duration timeout, long hertz);
    void stopPeriodic();
}
