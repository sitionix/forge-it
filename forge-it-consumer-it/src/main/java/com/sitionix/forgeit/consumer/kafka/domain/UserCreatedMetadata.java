package com.sitionix.forgeit.consumer.kafka.domain;

import lombok.Data;

@Data
public class UserCreatedMetadata {

    private String traceId;
    private String source;
}
