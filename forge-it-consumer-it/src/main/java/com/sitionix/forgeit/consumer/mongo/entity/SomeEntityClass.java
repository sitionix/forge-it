package com.sitionix.forgeit.consumer.mongo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SomeEntityClass {

    @Id
    private String id;

    private String name;

    private String description;
}
