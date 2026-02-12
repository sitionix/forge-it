package com.sitionix.forgeit.consumer.mongo;

import com.sitionix.forgeit.consumer.mongo.entity.SomeEntityClass;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MongoCleanupSmokeIT {

    @Autowired
    private MongoForgeItSupport forgeIt;

    @Test
    @Order(1)
    void step1_shouldCreateMongoDocument() {
        final List<SomeEntityClass> before = this.forgeIt.mongo()
                .get(SomeEntityClass.class)
                .getAll();

        assertThat(before).isEmpty();

        this.forgeIt.mongo()
                .create(SomeEntityClass.class)
                .body(SomeEntityClass.builder()
                        .name("cleanup-smoke")
                        .description("step-1")
                        .build());

        final List<SomeEntityClass> after = this.forgeIt.mongo()
                .get(SomeEntityClass.class)
                .getAll();

        assertThat(after).hasSize(1);
    }

    @Test
    @Order(2)
    void step2_shouldSeeEmptyCollectionIfCleanupWorks() {
        final List<SomeEntityClass> afterCleanup = this.forgeIt.mongo()
                .get(SomeEntityClass.class)
                .getAll();

        assertThat(afterCleanup).isEmpty();
    }
}
