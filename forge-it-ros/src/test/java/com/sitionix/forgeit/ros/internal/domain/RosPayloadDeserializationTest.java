package com.sitionix.forgeit.ros.internal.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.ros.api.RosPayloadDeserializer;
import java.nio.charset.StandardCharsets;
import com.sitionix.forgeit.ros.api.RosTopicContract;
import com.sitionix.forgeit.ros.internal.config.RosProperties;
import com.sitionix.forgeit.ros.internal.loader.RosLoader;
import com.sitionix.forgeit.ros.internal.port.RosSubscription;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class RosPayloadDeserializationTest {
    public record Status(String status) { }
    public static class StatusDeserializer implements RosPayloadDeserializer<Status> {
        public Status deserialize(byte[] payload, Class<Status> targetType) {
            try { return new ObjectMapper().readValue(payload, targetType); }
            catch (Exception ex) { throw new IllegalArgumentException("private payload", ex); }
        }
    }
    public static class BinaryDeserializer implements RosPayloadDeserializer<Status> {
        static byte[] receivedBytes;
        public Status deserialize(byte[] payload, Class<Status> targetType) {
            receivedBytes = payload;
            return new Status("IN_PROGRESS");
        }
    }
    public static class NullDeserializer implements RosPayloadDeserializer<Status> {
        public Status deserialize(byte[] payload, Class<Status> targetType) { return null; }
    }
    public static class PrivateConstructorDeserializer implements RosPayloadDeserializer<Status> {
        private PrivateConstructorDeserializer() { }
        public Status deserialize(byte[] payload, Class<Status> targetType) { return new Status("IN_PROGRESS"); }
    }
    public static class SlowDeserializer implements RosPayloadDeserializer<Status> {
        static AtomicLong clock;
        public Status deserialize(byte[] payload, Class<Status> targetType) {
            clock.addAndGet(Duration.ofSeconds(2).toNanos());
            return new Status("IN_PROGRESS");
        }
    }
    private final AtomicLong clock = new AtomicLong();
    private Class<? extends RosPayloadDeserializer<Status>> decoder = StatusDeserializer.class;
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicInteger received = new AtomicInteger();
    private boolean closed;
    private RosTopicContract contract() {
        return RosTopicContract.builder().topic("/alpha/radio/rx")
                .messageType("example_msgs/msg/TelemetryEvent")
                .payload("event", Status.class, decoder)
                .defaultExpectedMessage("inProgress.json").build();
    }
    private JsonNode message(String status) {
        var envelope = json.createObjectNode().put("from_id", 12);
        var bytes = envelope.putArray("event");
        for (byte value : ("{\"status\":\"" + status + "\"}").getBytes(StandardCharsets.UTF_8)) bytes.add(Byte.toUnsignedInt(value));
        return envelope;
    }
    private DefaultRosConsumeBuilder consume(java.util.function.Supplier<JsonNode> messages) {
        return new DefaultRosConsumeBuilder(contract(),
                new RosLoader(path -> "{\"from_id\":12,\"event\":{\"status\":\"IN_PROGRESS\"}}"),
                new MockEnvironment(), new RosProperties(), (topic, type, qos, timeout) -> new RosSubscription() {
                    public JsonNode next(Duration wait) { received.incrementAndGet(); return messages.get(); }
                    public void close() { closed = true; }
                }, clock::get);
    }
    @Test void comparesDecodedPayloadAndPreservesEnvelope() {
        consume(() -> message("IN_PROGRESS")).assertMessage();
        assertTrue(closed);
    }
    @Test void waitsForDecodedStatusWithoutChangingInput() {
        JsonNode waiting = message("WAITING");
        consume(() -> received.get() == 1 ? waiting : message("IN_PROGRESS"))
                .waitUntilAsserted(Duration.ofSeconds(1)).assertMessage();
        assertEquals(2, received.get());
        assertTrue(waiting.get("event").isArray());
        assertTrue(closed);
    }
    @Test void malformedPayloadFailsAndClosesWithoutLeakingPayload() {
        var invalid = json.createObjectNode().put("from_id",12);
        invalid.putArray("event").add(255);
        var failure = assertThrows(IllegalStateException.class, () -> consume(() -> invalid).assertMessage());
        assertTrue(closed);
        assertNull(failure.getCause());
        assertFalse(failure.toString().contains("255"));
    }
    @Test void missingAndInvalidBytesFailConsumption() {
        for (String encoded : new String[]{"null", "{}", "[-1]", "[256]", "[1.5]", "[2147483648]"}) {
            JsonNode invalid;
            try { invalid = json.readTree("{\"event\":" + encoded + "}"); }
            catch (Exception ex) { throw new AssertionError(ex); }
            assertThrows(IllegalStateException.class, () -> consume(() -> invalid).assertMessage());
            assertTrue(closed);
        }
        assertThrows(IllegalStateException.class,
                () -> consume(() -> json.createObjectNode()).assertMessage());
    }
    @Test void preservesAllUnsignedByteValuesForConsumerDecoder() {
        decoder = BinaryDeserializer.class;
        var envelope = json.createObjectNode().put("from_id", 12);
        envelope.putArray("event").add(0).add(127).add(128).add(255);
        consume(() -> envelope).assertMessage();
        assertArrayEquals(new byte[]{0, 127, (byte) 128, (byte) 255}, BinaryDeserializer.receivedBytes);
        assertEquals(255, envelope.get("event").get(3).intValue());
        assertTrue(closed);
    }
    @Test void decodingCannotExtendThroughoutDeadline() {
        decoder = SlowDeserializer.class;
        SlowDeserializer.clock = clock;
        assertThrows(AssertionError.class, () -> consume(() -> message("IN_PROGRESS"))
                .assertMessageThroughout(Duration.ofSeconds(1)));
        assertTrue(closed);
    }
    @Test void decodingCannotExtendAssertionDeadline() {
        decoder = SlowDeserializer.class;
        SlowDeserializer.clock = clock;
        assertThrows(AssertionError.class, () -> consume(() -> message("IN_PROGRESS"))
                .waitUntilAsserted(Duration.ofSeconds(1)).assertMessage());
        assertTrue(closed);
    }
    @Test void nullDecodedObjectFailsAndCloses() {
        decoder = NullDeserializer.class;
        assertThrows(IllegalStateException.class, () -> consume(() -> message("IN_PROGRESS")).assertMessage());
        assertTrue(closed);
    }
    @Test void unusableDecoderFailsBeforeSubscribing() {
        decoder = PrivateConstructorDeserializer.class;
        assertThrows(IllegalArgumentException.class, () -> consume(() -> message("IN_PROGRESS")));
        assertEquals(0, received.get());
    }
    @Test void contractRejectsInvalidDecoderDefinition() {
        assertThrows(IllegalArgumentException.class, () -> RosTopicContract.builder()
                .payload("../event", Status.class, StatusDeserializer.class));
        assertThrows(IllegalArgumentException.class, () -> RosTopicContract.builder()
                .payload("event", null, StatusDeserializer.class));
    }
    @Test void wrongSenderCannotSatisfyDecodedAssertion() {
        var wrongSender = (com.fasterxml.jackson.databind.node.ObjectNode) message("IN_PROGRESS");
        wrongSender.put("from_id",13);
        assertThrows(AssertionError.class, () -> consume(() -> wrongSender).assertMessage());
        assertTrue(closed);
    }
    @Test void decodingAlsoAppliesToThroughout() {
        assertThrows(AssertionError.class, () -> consume(() -> message("WAITING"))
                .assertMessageThroughout(Duration.ofSeconds(1)));
        assertTrue(closed);
    }
}
