package com.sitionix.forgeit.kafka.api;

public final class KafkaConsumerContractBuilder<T> {

    private Class<?> payloadType;
    private Class<?> envelopeType;
    private String topic;
    private String defaultExpectedPayloadName;
    private String defaultEnvelopeName;

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

    public <E> KafkaConsumerEnvelopeContractBuilder<E> defaultEnvelope(final Class<E> envelopeType,
                                                                       final String envelopeName) {
        this.configureEnvelope(envelopeType, envelopeName);
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
        if (this.envelopeType != null && (this.defaultEnvelopeName == null || this.defaultEnvelopeName.isBlank())) {
            throw new IllegalStateException("envelopeName must be provided");
        }
        final Class<?> rootType = this.envelopeType != null ? this.envelopeType : this.payloadType;
        return KafkaContract.createContract(this.topic,
                rootType,
                this.payloadType,
                null,
                this.defaultExpectedPayloadName,
                this.defaultEnvelopeName,
                this.envelopeType);
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

    private void configureEnvelope(final Class<?> envelopeType, final String envelopeName) {
        final DefaultEnvelope defaultEnvelope = new DefaultEnvelope(envelopeType, envelopeName);
        this.assignEnvelopeType(defaultEnvelope.envelopeType());
        this.defaultEnvelopeName = defaultEnvelope.envelopeName();
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
        public <U> KafkaConsumerEnvelopeContractBuilder<T> defaultExpectedPayload(final Class<U> payloadType,
                                                                                  final String payloadName) {
            this.delegate.defaultExpectedPayload(payloadType, payloadName);
            return this;
        }

        @Override
        public KafkaConsumerEnvelopeContractBuilder<T> defaultEnvelope(final Class<T> envelopeType,
                                                                       final String envelopeName) {
            this.delegate.configureEnvelope(envelopeType, envelopeName);
            return this;
        }

        @Override
        public KafkaContract<T> build() {
            return castContract(this.delegate.build());
        }
    }
}
