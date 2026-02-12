package com.sitionix.forgeit.consumer.mongo.controller;

import com.sitionix.forgeit.consumer.mongo.entity.SomeEntityClass;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mongo")
@RequiredArgsConstructor
@ConditionalOnBean(MongoTemplate.class)
@ConditionalOnProperty(prefix = "forge-it.modules.mongodb", name = "enabled", havingValue = "true")
public class MongoController {

    private final MongoRepository mongoRepository;

    @PostMapping("/entities")
    public ResponseEntity<SomeEntityClass> create(@RequestBody final SomeEntityClass request) {
        return ResponseEntity.ok(this.mongoRepository.create(request));
    }
}
