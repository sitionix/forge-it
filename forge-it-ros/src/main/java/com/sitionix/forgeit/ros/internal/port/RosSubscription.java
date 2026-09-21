package com.sitionix.forgeit.ros.internal.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;

/** A stream of new messages whose reads share the caller's remaining deadline. */
public interface RosSubscription extends AutoCloseable {

    JsonNode next(Duration remaining);

    @Override
    void close();
}
