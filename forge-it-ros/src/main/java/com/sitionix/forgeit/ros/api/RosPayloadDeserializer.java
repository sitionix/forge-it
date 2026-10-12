package com.sitionix.forgeit.ros.api;

/** Consumer-owned decoding of a ROS uint8[] payload into a typed assertion object.
 * Implementations must expose a public no-argument constructor.
 */
@FunctionalInterface
public interface RosPayloadDeserializer<T> {
    T deserialize(byte[] payload, Class<T> targetType);
}
