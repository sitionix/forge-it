package com.sitionix.forgeit.ros.internal.transport;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;

public interface RosSubscription extends AutoCloseable {
    JsonNode next(Duration remaining);
    @Override void close();
}
