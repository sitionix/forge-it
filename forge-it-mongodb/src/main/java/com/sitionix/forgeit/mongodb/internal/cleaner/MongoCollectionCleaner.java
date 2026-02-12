package com.sitionix.forgeit.mongodb.internal.cleaner;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.clean.DbCleaner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class MongoCollectionCleaner implements DbCleaner {

    private final MongoTemplate mongoTemplate;

    public MongoCollectionCleaner(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void clearTables(final List<DbContract<?>> contracts) {
        this.reset();
    }

    public void reset() {
        this.mongoTemplate.getDb().drop();
    }
}
