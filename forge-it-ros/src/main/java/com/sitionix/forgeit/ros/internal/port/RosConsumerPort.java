package com.sitionix.forgeit.ros.internal.port;

import com.sitionix.forgeit.ros.api.RosQos;

import java.time.Duration;

public interface RosConsumerPort {

    RosSubscription subscribe(String topic, String messageType, RosQos qos, Duration timeout);
}
