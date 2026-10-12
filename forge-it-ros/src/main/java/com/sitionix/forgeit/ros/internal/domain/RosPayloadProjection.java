package com.sitionix.forgeit.ros.internal.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sitionix.forgeit.ros.api.RosPayloadDeserializer;
import com.sitionix.forgeit.ros.api.RosTopicContract;

/** Keeps wire-format knowledge in the consumer's deserializer. */
final class RosPayloadProjection<T> {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final RosTopicContract.Payload<T> payload;
    private final RosPayloadDeserializer<T> deserializer;

    RosPayloadProjection(RosTopicContract.Payload<T> payload) {
        this.payload = payload;
        try {
            this.deserializer = payload.deserializer().getConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            throw new IllegalArgumentException("ROS payload deserializer requires a public no-argument constructor");
        }
    }

    JsonNode decode(JsonNode envelope) {
        try {
            JsonNode encoded = envelope.get(payload.field());
            if (!envelope.isObject() || encoded == null || !encoded.isArray()) {
                throw new IllegalArgumentException();
            }
            byte[] bytes = new byte[encoded.size()];
            for (int index = 0; index < bytes.length; index++) {
                JsonNode value = encoded.get(index);
                if (!value.isIntegralNumber() || !value.canConvertToInt()
                        || value.intValue() < 0 || value.intValue() > 255) {
                    throw new IllegalArgumentException();
                }
                bytes[index] = (byte) value.intValue();
            }
            T decoded = deserializer.deserialize(bytes, payload.type());
            if (!payload.type().isInstance(decoded)) throw new IllegalArgumentException();
            ObjectNode copy = envelope.deepCopy();
            copy.set(payload.field(), JSON.valueToTree(decoded));
            return copy;
        } catch (RuntimeException ex) {
            // Decoder exceptions and Jackson failures can contain raw payload or credentials.
            throw new IllegalStateException("ROS payload could not be deserialized to the contract type");
        }
    }
}
