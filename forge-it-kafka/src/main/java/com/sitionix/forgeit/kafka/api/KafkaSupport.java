package com.sitionix.forgeit.kafka.api;

import com.sitionix.forgeit.core.internal.feature.FeatureContextHolder;
import com.sitionix.forgeit.core.marker.FeatureSupport;

/**
 * Public contract describing Kafka capabilities exposed to ForgeIT clients.
 */
public interface KafkaSupport extends FeatureSupport {

    default KafkaBridge kafka() {
        return FeatureContextHolder.getBean(KafkaBridge.class);
    }
}
