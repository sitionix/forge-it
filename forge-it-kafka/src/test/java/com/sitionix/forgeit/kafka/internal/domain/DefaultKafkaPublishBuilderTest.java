package com.sitionix.forgeit.kafka.internal.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.internal.config.KafkaProperties;
import com.sitionix.forgeit.kafka.internal.loader.KafkaLoader;
import com.sitionix.forgeit.kafka.internal.port.KafkaPublisherPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultKafkaPublishBuilderTest {

    @Test
    void shouldPublishEnvelopeWithDefaultPayloadAndMetadataMutations() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final FakeJsonLoader loader = new FakeJsonLoader();
        loader.put("/default/payload/defaultPayload.json", new Payload("default-user"));
        loader.put("/default/metadata/defaultMetadata.json", new Metadata("default-trace"));
        final KafkaLoader kafkaLoader = createKafkaLoader(loader);

        final KafkaContract<Envelope> contract = KafkaContract.producerContract()
                .topic("topic")
                .defaultEnvelope(Envelope.class)
                .defaultPayload(Payload.class, "defaultPayload.json")
                .defaultMetadata(Metadata.class, "defaultMetadata.json")
                .build();

        final CapturingPublisher publisher = new CapturingPublisher();
        final DefaultKafkaPublishBuilder<Envelope> builder = new DefaultKafkaPublishBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                publisher);

        builder.payload(envelope -> envelope.getPayload().setUserId("mutated-user"))
                .metadata(envelope -> envelope.getMetadata().setTraceId("trace-1"))
                .envelope(envelope -> envelope.setProducedAt(42L))
                .send();

        final Envelope published = objectMapper.readValue((String) publisher.payloadValue, Envelope.class);
        assertThat(published.getPayload().getUserId()).isEqualTo("mutated-user");
        assertThat(published.getMetadata().getTraceId()).isEqualTo("trace-1");
        assertThat(published.getProducedAt()).isEqualTo(42L);
    }

    @Test
    void shouldPublishOverridePayloadFixture() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final FakeJsonLoader loader = new FakeJsonLoader();
        loader.put("/payload/overridePayload.json", new Payload("override-user"));
        loader.put("/default/metadata/defaultMetadata.json", new Metadata("default-trace"));
        final KafkaLoader kafkaLoader = createKafkaLoader(loader);

        final KafkaContract<Envelope> contract = KafkaContract.producerContract()
                .topic("topic")
                .defaultEnvelope(Envelope.class)
                .defaultPayload(Payload.class, "defaultPayload.json")
                .defaultMetadata(Metadata.class, "defaultMetadata.json")
                .build();

        final CapturingPublisher publisher = new CapturingPublisher();
        final DefaultKafkaPublishBuilder<Envelope> builder = new DefaultKafkaPublishBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                publisher);

        builder.payload("overridePayload.json")
                .send();

        final Envelope published = objectMapper.readValue((String) publisher.payloadValue, Envelope.class);
        assertThat(published.getPayload().getUserId()).isEqualTo("override-user");
        assertThat(published.getMetadata().getTraceId()).isEqualTo("default-trace");
    }

    @Test
    void shouldRejectMissingDefaultPayloadWhenMutating() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final FakeJsonLoader loader = new FakeJsonLoader();
        final KafkaLoader kafkaLoader = createKafkaLoader(loader);

        final KafkaContract<Envelope> contract = KafkaContract.builder(Envelope.class)
                .topic("topic")
                .payloadType(Payload.class)
                .build();

        final DefaultKafkaPublishBuilder<Envelope> builder = new DefaultKafkaPublishBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                new CapturingPublisher());

        assertThatThrownBy(() -> builder.payload(envelope -> envelope.getPayload().setUserId("x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kafka default payload is not configured");
    }

    @Test
    void shouldPublishUsingKafkaSerializerClass() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final FakeJsonLoader loader = new FakeJsonLoader();
        loader.put("/default/payload/defaultPayload.json", new Payload("default-user"));
        final KafkaLoader kafkaLoader = createKafkaLoader(loader);

        final KafkaContract<Envelope> contract = KafkaContract.producerContract()
                .topic("topic")
                .defaultEnvelope(Envelope.class)
                .defaultPayload(Payload.class, "defaultPayload.json")
                .payloadSerializer(ConstantPayloadSerializer.class)
                .build();

        final CapturingPublisher publisher = new CapturingPublisher();
        final DefaultKafkaPublishBuilder<Envelope> builder = new DefaultKafkaPublishBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                publisher);

        builder.send();

        assertThat(publisher.payloadValue).isInstanceOf(byte[].class);
        final String encoded = new String((byte[]) publisher.payloadValue, StandardCharsets.UTF_8);
        assertThat(encoded).isEqualTo("serialized");
    }

    @Test
    void shouldSendAndVerifyEnvelope() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final FakeJsonLoader loader = new FakeJsonLoader();
        loader.put("/default/payload/defaultPayload.json", new Payload("default-user"));
        loader.put("/default/metadata/defaultMetadata.json", new Metadata("default-trace"));
        final KafkaLoader kafkaLoader = createKafkaLoader(loader);

        final KafkaContract<Envelope> contract = KafkaContract.producerContract()
                .topic("topic")
                .defaultEnvelope(Envelope.class)
                .defaultPayload(Payload.class, "defaultPayload.json")
                .defaultMetadata(Metadata.class, "defaultMetadata.json")
                .build();

        final CapturingPublisher publisher = new CapturingPublisher();
        final DefaultKafkaPublishBuilder<Envelope> builder = new DefaultKafkaPublishBuilder<>(contract,
                kafkaLoader,
                objectMapper,
                publisher);
        final AtomicBoolean verified = new AtomicBoolean(false);

        builder.payload(envelope -> envelope.getPayload().setUserId("mutated-user"))
                .metadata(envelope -> envelope.getMetadata().setTraceId("trace-1"))
                .sendAndVerify(Duration.ofSeconds(1), envelope -> {
                    verified.set(true);
                    assertThat(envelope.getPayload().getUserId()).isEqualTo("mutated-user");
                    assertThat(envelope.getMetadata().getTraceId()).isEqualTo("trace-1");
                });

        assertThat(verified.get()).isTrue();
        assertThat(publisher.payloadValue).isNotNull();
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

    static final class CapturingPublisher implements KafkaPublisherPort {
        private Object payloadValue;

        @Override
        public <T> void publish(final KafkaContract<T> contract, final Object payload, final String key) {
            this.payloadValue = payload;
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

    static final class ConstantPayloadSerializer implements org.apache.kafka.common.serialization.Serializer<Object> {
        @Override
        public byte[] serialize(final String topic, final Object data) {
            return "serialized".getBytes(StandardCharsets.UTF_8);
        }
    }

    static final class Envelope {
        private Payload payload;
        private Metadata metadata;
        private long producedAt;

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

        public long getProducedAt() {
            return producedAt;
        }

        public void setProducedAt(final long producedAt) {
            this.producedAt = producedAt;
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
