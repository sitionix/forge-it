package com.sitionix.forgeit.kafka.internal.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.config.KafkaProperties;
import com.sitionix.forgeit.kafka.internal.loader.KafkaLoader;
import com.sitionix.forgeit.kafka.internal.port.KafkaConsumerPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultKafkaConsumeBuilderTest {

    @Test
    void shouldAssertPayloadAndMetadataUsingSingleConsume() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final FakeJsonLoader loader = new FakeJsonLoader();
        loader.put("/default/expected/defaultPayload.json", new Payload("default-user"));
        loader.put("/default/metadata/defaultMetadata.json", new Metadata("default-trace"));
        final KafkaLoader kafkaLoader = createKafkaLoader(loader);

        final KafkaContract<Envelope> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultEnvelope(Envelope.class)
                .defaultExpectedPayload(Payload.class, "defaultPayload.json")
                .defaultMetadata(Metadata.class, "defaultMetadata.json")
                .build();

        final Envelope actualEnvelope = new Envelope();
        actualEnvelope.setPayload(new Payload("mutated-user"));
        actualEnvelope.setMetadata(new Metadata("trace-1"));
        final String payloadJson = objectMapper.writeValueAsString(actualEnvelope);

        final CapturingConsumer consumer = new CapturingConsumer(payloadJson);
        final DefaultKafkaConsumeBuilder<Envelope> builder = new DefaultKafkaConsumeBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                consumer);

        builder.await(Duration.ofSeconds(1));
        builder.assertPayload(envelope -> envelope.getPayload().setUserId("mutated-user"));
        builder.assertMetadata(envelope -> envelope.getMetadata().setTraceId("trace-1"));

        assertThat(consumer.calls).isEqualTo(1);
    }

    @Test
    void shouldAssertAnyWhenMessageConsumed() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KafkaLoader kafkaLoader = createKafkaLoader(new FakeJsonLoader());

        final KafkaContract<Payload> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultExpectedPayload(Payload.class, "defaultPayload.json")
                .build();

        final CapturingConsumer consumer = new CapturingConsumer("payload");
        final DefaultKafkaConsumeBuilder<Payload> builder = new DefaultKafkaConsumeBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                consumer);

        builder.assertAny();

        assertThat(consumer.calls).isEqualTo(1);
    }

    @Test
    void shouldAssertNoneWhenNoMessageConsumed() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final KafkaLoader kafkaLoader = createKafkaLoader(new FakeJsonLoader());

        final KafkaContract<Payload> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultExpectedPayload(Payload.class, "defaultPayload.json")
                .build();

        final CapturingConsumer consumer = new CapturingConsumer(null);
        final DefaultKafkaConsumeBuilder<Payload> builder = new DefaultKafkaConsumeBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                consumer);

        builder.assertNone();

        assertThat(consumer.calls).isEqualTo(1);
    }

    @Test
    void shouldIgnoreMetadataFieldsWhenAsserting() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final FakeJsonLoader loader = new FakeJsonLoader();
        loader.put("/default/expected/defaultPayload.json", new Payload("default-user"));
        loader.put("/default/metadata/defaultMetadata.json", new Metadata("default-trace"));
        final KafkaLoader kafkaLoader = createKafkaLoader(loader);

        final KafkaContract<Envelope> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultEnvelope(Envelope.class)
                .defaultExpectedPayload(Payload.class, "defaultPayload.json")
                .defaultMetadata(Metadata.class, "defaultMetadata.json")
                .build();

        final Envelope actualEnvelope = new Envelope();
        actualEnvelope.setPayload(new Payload("default-user"));
        actualEnvelope.setMetadata(new Metadata("actual-trace"));
        final String payloadJson = objectMapper.writeValueAsString(actualEnvelope);

        final CapturingConsumer consumer = new CapturingConsumer(payloadJson);
        final DefaultKafkaConsumeBuilder<Envelope> builder = new DefaultKafkaConsumeBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                consumer);

        builder.ignoreFields("traceId")
                .assertMetadata(envelope -> {
                });
    }

    @Test
    void shouldUseKafkaDeserializerClass() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final FakeJsonLoader loader = new FakeJsonLoader();
        loader.put("/default/expected/defaultPayload.json", new Payload("default-user"));
        final KafkaLoader kafkaLoader = createKafkaLoader(loader);

        final KafkaContract<Payload> contract = KafkaContract.consumerContract()
                .topic("topic")
                .defaultExpectedPayload(Payload.class, "defaultPayload.json")
                .payloadDeserializer(PayloadDeserializer.class)
                .build();

        final byte[] payloadBytes = "default-user".getBytes(StandardCharsets.UTF_8);
        final CapturingConsumer consumer = new CapturingConsumer(payloadBytes);
        final DefaultKafkaConsumeBuilder<Payload> builder = new DefaultKafkaConsumeBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                consumer);

        builder.assertPayload();
    }

    private static KafkaLoader createKafkaLoader(final FakeJsonLoader loader) {
        final KafkaProperties properties = new KafkaProperties();
        final KafkaProperties.Path path = new KafkaProperties.Path();
        path.setPayload("/payload");
        path.setDefaultPayload("/default/payload");
        path.setExpected("/expected");
        path.setDefaultExpected("/default/expected");
        path.setMetadata("/metadata");
        path.setDefaultMetadata("/default/metadata");
        properties.setPath(path);
        return new KafkaLoader(new SingletonProvider<>(loader), properties);
    }

    static final class CapturingConsumer implements KafkaConsumerPort {
        private final Object payload;
        private int calls;

        CapturingConsumer(final Object payload) {
            this.payload = payload;
        }

        @Override
        public <T> Object consume(final KafkaContract<T> contract, final Duration timeout) {
            this.calls += 1;
            return this.payload;
        }

        @Override
        public <T> Object consumeIfPresent(final KafkaContract<T> contract, final Duration timeout) {
            this.calls += 1;
            return this.payload;
        }
    }

    static final class FakeJsonLoader implements JsonLoader {
        private final Map<String, Object> data = new HashMap<>();
        private String basePath;

        void put(final String path, final Object value) {
            this.data.put(path, value);
        }

        @Override
        public <T> T getFromFile(final String fileName, final Class<T> tClass) {
            final Object value = this.data.get(this.basePath + "/" + fileName);
            if (value == null) {
                throw new IllegalStateException("Fixture not found: " + this.basePath + "/" + fileName);
            }
            return tClass.cast(value);
        }

        @Override
        public String getFromFile(final String fileName) {
            final Object value = this.data.get(this.basePath + "/" + fileName);
            return value == null ? null : value.toString();
        }

        @Override
        public void setBasePath(final String basePath) {
            this.basePath = basePath;
        }
    }

    static final class SingletonProvider<T> implements ObjectProvider<T> {
        private final T instance;

        SingletonProvider(final T instance) {
            this.instance = instance;
        }

        @Override
        public T getObject(final Object... args) {
            return this.instance;
        }

        @Override
        public T getObject() {
            return this.instance;
        }

        @Override
        public T getIfAvailable() {
            return this.instance;
        }

        @Override
        public T getIfUnique() {
            return this.instance;
        }
    }

    static final class Envelope {
        private Payload payload;
        private Metadata metadata;

        public Payload getPayload() {
            return payload;
        }

        public void setPayload(final Payload payload) {
            this.payload = payload;
        }

        public Metadata getMetadata() {
            return metadata;
        }

        public void setMetadata(final Metadata metadata) {
            this.metadata = metadata;
        }
    }

    static final class Payload {
        private String userId;

        Payload() {
        }

        Payload(final String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(final String userId) {
            this.userId = userId;
        }
    }

    static final class PayloadDeserializer implements org.apache.kafka.common.serialization.Deserializer<Payload> {
        @Override
        public Payload deserialize(final String topic, final byte[] data) {
            if (data == null) {
                return null;
            }
            final String userId = new String(data, StandardCharsets.UTF_8);
            return new Payload(userId);
        }
    }

    static final class Metadata {
        private String traceId;

        Metadata() {
        }

        Metadata(final String traceId) {
            this.traceId = traceId;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(final String traceId) {
            this.traceId = traceId;
        }
    }
}
