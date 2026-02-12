package com.sitionix.forgeit.consumer.mongo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "some_entities")
public class SomeEntityClass {

    @Id
    private String id;

    private String name;

    private String description;
}
