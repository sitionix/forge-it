package com.sitionix.forgeit.consumer.kafka.domain;

import lombok.Data;

@Data
public class UserCreatedEvent {

    private String userId;
    private String email;
}
