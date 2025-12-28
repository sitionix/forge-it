package com.sitionix.forgeit.kafka.api;

public final class KafkaConsumerContractBuilder<T> {

    private Class<?> payloadType;
    private Class<?> envelopeType;
    private Class<?> metadataType;
    private String topic;
    private String defaultExpectedPayloadName;
    private String defaultEnvelopeName;
    private String defaultMetadataName;
    private String groupId;

    private KafkaConsumerContractBuilder(final Class<?> payloadType) {
        this.payloadType = payloadType;
    }

    public static <T> KafkaConsumerContractBuilder<T> forPayload(final Class<T> payloadType) {
        if (payloadType == null) {
            throw new IllegalArgumentException("payloadType must be provided");
        }
        return new KafkaConsumerContractBuilder<>(payloadType);
    }

    public static KafkaConsumerContractBuilder<?> empty() {
        return new KafkaConsumerContractBuilder<>(null);
    }

    public KafkaConsumerContractBuilder<T> topic(final String topic) {
        this.topic = topic;
        return this;
    }

    public KafkaConsumerContractBuilder<T> topicFromProperty(final String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            throw new IllegalArgumentException("propertyKey must be provided");
        }
        this.topic = "${" + propertyKey + "}";
        return this;
    }

    public KafkaConsumerContractBuilder<T> defaultExpectedPayload(final String payloadName) {
        this.defaultExpectedPayloadName = payloadName;
        return this;
    }

    public KafkaConsumerContractBuilder<T> groupId(final String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId must be provided");
        }
        this.groupId = groupId;
        return this;
    }

    public <M> KafkaConsumerContractBuilder<T> defaultMetadata(final Class<M> metadataType,
                                                               final String metadataName) {
        this.configureMetadata(metadataType, metadataName);
        return this;
    }

    public <E> KafkaConsumerEnvelopeContractBuilder<E> defaultEnvelope(final Class<E> envelopeType) {
        this.configureEnvelope(envelopeType);
        return new EnvelopeContractBuilder<>(this);
    }

    public <U> KafkaConsumerContractBuilder<U> defaultExpectedPayload(final Class<U> payloadType,
                                                                      final String payloadName) {
        this.assignPayloadType(payloadType);
        this.defaultExpectedPayloadName = payloadName;
        @SuppressWarnings("unchecked")
        final KafkaConsumerContractBuilder<U> typedBuilder = (KafkaConsumerContractBuilder<U>) this;
        return typedBuilder;
    }

    public KafkaContract<T> build() {
        if (this.topic == null || this.topic.isBlank()) {
            throw new IllegalStateException("Kafka topic must be provided");
        }
        if (this.payloadType == null) {
            throw new IllegalStateException("payloadType must be provided");
        }
        final Class<?> rootType = this.envelopeType != null ? this.envelopeType : this.payloadType;
        return KafkaContract.createContract(this.topic,
                rootType,
                this.payloadType,
                null,
                this.defaultExpectedPayloadName,
                this.groupId,
                this.defaultEnvelopeName,
                this.envelopeType,
                this.defaultMetadataName,
                this.metadataType);
    }

    private void assignPayloadType(final Class<?> payloadType) {
        if (payloadType == null) {
            throw new IllegalArgumentException("payloadType must be provided");
        }
        if (this.payloadType != null && !this.payloadType.equals(payloadType)) {
            throw new IllegalStateException("payloadType is already set");
        }
        this.payloadType = payloadType;
    }

    private void assignEnvelopeType(final Class<?> envelopeType) {
        if (envelopeType == null) {
            throw new IllegalArgumentException("envelopeType must be provided");
        }
        if (this.envelopeType != null && !this.envelopeType.equals(envelopeType)) {
            throw new IllegalStateException("envelopeType is already set");
        }
        this.envelopeType = envelopeType;
    }

    private void assignMetadataType(final Class<?> metadataType) {
        if (metadataType == null) {
            throw new IllegalArgumentException("metadataType must be provided");
        }
        if (this.metadataType != null && !this.metadataType.equals(metadataType)) {
            throw new IllegalStateException("metadataType is already set");
        }
        this.metadataType = metadataType;
    }

    private void configureMetadata(final Class<?> metadataType, final String metadataName) {
        if (metadataName == null || metadataName.isBlank()) {
            throw new IllegalArgumentException("metadataName must be provided");
        }
        this.assignMetadataType(metadataType);
        this.defaultMetadataName = metadataName;
    }

    private void configureEnvelope(final Class<?> envelopeType) {
        this.assignEnvelopeType(envelopeType);
        this.defaultEnvelopeName = null;
    }


    private static <T> KafkaContract<T> castContract(final KafkaContract<?> contract) {
        @SuppressWarnings("unchecked")
        final KafkaContract<T> typed = (KafkaContract<T>) contract;
        return typed;
    }

    private static final class EnvelopeContractBuilder<T> implements KafkaConsumerEnvelopeContractBuilder<T> {

        private final KafkaConsumerContractBuilder<?> delegate;

        private EnvelopeContractBuilder(final KafkaConsumerContractBuilder<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public KafkaConsumerEnvelopeContractBuilder<T> topic(final String topic) {
            this.delegate.topic(topic);
            return this;
        }

        @Override
        public KafkaConsumerEnvelopeContractBuilder<T> topicFromProperty(final String propertyKey) {
            this.delegate.topicFromProperty(propertyKey);
            return this;
        }

        @Override
        public KafkaConsumerEnvelopeContractBuilder<T> defaultExpectedPayload(final String payloadName) {
            this.delegate.defaultExpectedPayload(payloadName);
            return this;
        }

        @Override
        public KafkaConsumerEnvelopeContractBuilder<T> groupId(final String groupId) {
            this.delegate.groupId(groupId);
            return this;
        }

        @Override
        public <U> KafkaConsumerEnvelopeContractBuilder<T> defaultExpectedPayload(final Class<U> payloadType,
                                                                                  final String payloadName) {
            this.delegate.defaultExpectedPayload(payloadType, payloadName);
            return this;
        }

        @Override
        public <M> KafkaConsumerEnvelopeContractBuilder<T> defaultMetadata(final Class<M> metadataType,
                                                                           final String metadataName) {
            this.delegate.defaultMetadata(metadataType, metadataName);
            return this;
        }

        @Override
        public KafkaConsumerEnvelopeContractBuilder<T> defaultEnvelope(final Class<T> envelopeType) {
            this.delegate.configureEnvelope(envelopeType);
            return this;
        }

        @Override
        public KafkaContract<T> build() {
            return castContract(this.delegate.build());
        }
    }
}
