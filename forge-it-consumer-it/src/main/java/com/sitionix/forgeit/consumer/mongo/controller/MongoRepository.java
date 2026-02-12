package com.sitionix.forgeit.consumer.mongo.controller;

import com.sitionix.forgeit.consumer.mongo.entity.SomeEntityClass;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(MongoTemplate.class)
@ConditionalOnProperty(prefix = "forge-it.modules.mongodb", name = "enabled", havingValue = "true")
public class MongoRepository {

    private final ObjectProvider<MongoTemplate> mongoTemplate;

    public SomeEntityClass create(final SomeEntityClass request) {
        final MongoTemplate template = this.mongoTemplate.getIfAvailable();
        if (template == null) {
            throw new IllegalStateException("MongoDB support is not enabled for this test context");
        }
        return template.save(request);
    }
}
