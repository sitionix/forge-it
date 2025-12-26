package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class KafkaBridgeIT {

    @Autowired
    private KafkaItSupport support;

    @Test
    @DisplayName("Given Kafka support When bridge is called Then it logs the producer bridge")
    void givenKafkaSupport_whenBridgeIsCalled_thenItLogsProducerBridge() {
        this.support.kafka().bridge();
    }
}
