package com.sitionix.forgeit.mongodb.internal.cleaner;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public final class MongoCollectionCleaner {

    private final MongoTemplate mongoTemplate;

    public MongoCollectionCleaner(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void reset() {
        this.mongoTemplate.getDb().drop();
    }
}
