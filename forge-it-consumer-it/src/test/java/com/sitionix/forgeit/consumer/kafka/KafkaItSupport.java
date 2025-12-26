package com.sitionix.forgeit.consumer.kafka;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.kafka.api.KafkaSupport;

@ForgeFeatures(KafkaSupport.class)
public interface KafkaItSupport extends ForgeIT {
}
