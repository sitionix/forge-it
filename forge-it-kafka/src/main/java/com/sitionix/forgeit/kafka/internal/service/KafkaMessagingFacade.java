package com.sitionix.forgeit.kafka.internal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.kafka.api.KafkaConsumeBuilder;
import com.sitionix.forgeit.kafka.api.KafkaContract;
import com.sitionix.forgeit.kafka.api.KafkaMessaging;
import com.sitionix.forgeit.kafka.api.KafkaPublishBuilder;
import com.sitionix.forgeit.kafka.internal.domain.DefaultKafkaConsumeBuilder;
import com.sitionix.forgeit.kafka.internal.domain.DefaultKafkaPublishBuilder;
import com.sitionix.forgeit.kafka.internal.loader.KafkaLoader;
import com.sitionix.forgeit.kafka.internal.port.KafkaConsumerPort;
import com.sitionix.forgeit.kafka.internal.port.KafkaPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaMessagingFacade implements KafkaMessaging {

    private final KafkaLoader kafkaLoader;
    private final ObjectMapper objectMapper;
    private final KafkaPublisherPort publisherPort;
    private final KafkaConsumerPort consumerPort;

    @Override
    public <T> KafkaPublishBuilder<T> publish(final KafkaContract<T> contract) {
        return new DefaultKafkaPublishBuilder<>(contract,
                this.kafkaLoader.payloads(),
                this.objectMapper,
                this.publisherPort);
    }

    @Override
    public <T> KafkaConsumeBuilder<T> consume(final KafkaContract<T> contract) {
        return new DefaultKafkaConsumeBuilder<>(contract,
                this.kafkaLoader.payloads(),
                this.objectMapper,
                this.consumerPort);
    }
}
