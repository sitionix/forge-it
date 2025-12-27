package com.sitionix.forgeit.kafka.api;

public final class KafkaProducerContractBuilder<T> {

    private Class<?> payloadType;
    private Class<?> envelopeType;
    private String topic;
    private String defaultPayloadName;
    private String defaultEnvelopeName;

    private KafkaProducerContractBuilder(final Class<?> payloadType) {
        this.payloadType = payloadType;
    }

    public static KafkaProducerContractBuilder<?> empty() {
        return new KafkaProducerContractBuilder<>(null);
    }

    public KafkaProducerContractBuilder<T> topic(final String topic) {
        this.topic = topic;
        return this;
    }

    public KafkaProducerContractBuilder<T> topicFromProperty(final String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            throw new IllegalArgumentException("propertyKey must be provided");
        }
        this.topic = "${" + propertyKey + "}";
        return this;
    }

    public <U> KafkaProducerContractBuilder<U> defaultPayload(final Class<U> payloadType, final String payloadName) {
        this.assignPayloadType(payloadType);
        this.defaultPayloadName = payloadName;
        @SuppressWarnings("unchecked")
        final KafkaProducerContractBuilder<U> typedBuilder = (KafkaProducerContractBuilder<U>) this;
        return typedBuilder;
    }

    public <E> KafkaProducerEnvelopeContractBuilder<E> defaultEnvelope(final Class<E> envelopeType,
                                                                       final String envelopeName) {
        this.configureEnvelope(envelopeType, envelopeName);
        return new EnvelopeContractBuilder<>(this);
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
                this.defaultPayloadName,
                null,
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

    private static final class EnvelopeContractBuilder<T> implements KafkaProducerEnvelopeContractBuilder<T> {

        private final KafkaProducerContractBuilder<?> delegate;

        private EnvelopeContractBuilder(final KafkaProducerContractBuilder<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public KafkaProducerEnvelopeContractBuilder<T> topic(final String topic) {
            this.delegate.topic(topic);
            return this;
        }

        @Override
        public KafkaProducerEnvelopeContractBuilder<T> topicFromProperty(final String propertyKey) {
            this.delegate.topicFromProperty(propertyKey);
            return this;
        }

        @Override
        public <U> KafkaProducerEnvelopeContractBuilder<T> defaultPayload(final Class<U> payloadType,
                                                                          final String payloadName) {
            this.delegate.defaultPayload(payloadType, payloadName);
            return this;
        }

        @Override
        public KafkaProducerEnvelopeContractBuilder<T> defaultEnvelope(final Class<T> envelopeType,
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
