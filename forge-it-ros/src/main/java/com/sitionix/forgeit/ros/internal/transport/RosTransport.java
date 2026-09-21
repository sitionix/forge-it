package com.sitionix.forgeit.ros.internal.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.sitionix.forgeit.ros.api.RosQos;
import java.time.Duration;

public interface RosTransport extends AutoCloseable {
    RosSubscription subscribe(String topic, String messageType, RosQos qos, Duration timeout);
    void publish(String topic, String messageType, RosQos qos, JsonNode message, Duration timeout);
    @Override void close();
}
