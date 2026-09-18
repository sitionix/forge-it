package com.sitionix.forgeit.ros.api;

/** The supported ROS 2 QoS subset. Unsupported values are rejected, never substituted. */
public record RosQos(Reliability reliability, Durability durability, History history, int depth) {
    public enum Reliability { RELIABLE, BEST_EFFORT }
    public enum Durability { VOLATILE, TRANSIENT_LOCAL }
    public enum History { KEEP_LAST }
    public RosQos {
        if (reliability == null || durability == null || history == null || depth <= 0) {
            throw new IllegalArgumentException("ROS QoS requires supported policies and positive depth");
        }
    }
    public static RosQos reliableVolatile(int depth) {
        return new RosQos(Reliability.RELIABLE, Durability.VOLATILE, History.KEEP_LAST, depth);
    }
}
