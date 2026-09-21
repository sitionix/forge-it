package com.sitionix.forgeit.ros.api;

public interface RosPublishBuilder {
    RosPublishBuilder message(String fixture);
    void publish();
    void publishDefault();
}
