package com.sitionix.forgeit.consumer.kafka.domain;

import lombok.Data;

@Data
public class UserEnvelope {

    private UserCreatedEvent payload;
    private UserCreatedMetadata metadata;
    private long producedAt;
}
