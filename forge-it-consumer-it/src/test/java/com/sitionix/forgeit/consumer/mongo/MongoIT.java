package com.sitionix.forgeit.consumer.mongo;

import com.sitionix.forgeit.consumer.mongo.entity.SomeEntityClass;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.UUID;

@IntegrationTest
class MongoIT {

    @Autowired
    private MongoForgeItSupport forgeIt;

    @Test
    @DisplayName("Given JSON body when creating Mongo entity then verifies through shared assertion builders")
    void givenJsonBody_whenCreatingMongoEntity_thenVerifiesThroughSharedAssertionBuilders() {
        final DbEntityHandle<SomeEntityClass> createdEntity = this.forgeIt.mongo()
                .create(SomeEntityClass.class)
                .body("some_entity.json");

        this.forgeIt.mongo()
                .assertEntity(createdEntity)
                .withJson("some_entity.json")
                .ignoreFields("id")
                .assertMatchesStrict();

        this.forgeIt.mongo()
                .assertEntities(SomeEntityClass.class)
                .ignoreFields("id")
                .hasSize(1)
                .containsWithJsonsStrict("some_entity.json");
    }

    @Test
    @DisplayName("Given JSON body when mutating Mongo entity then persisted value is updated")
    void givenJsonBody_whenMutatingMongoEntity_thenPersistedValueIsUpdated() {
        final String mutatedName = "json-mutate-" + UUID.randomUUID();

        this.forgeIt.mongo()
                .create(SomeEntityClass.class)
                .body("some_entity.json")
                .mutate(entity -> entity.setName(mutatedName));

        this.forgeIt.mongo()
                .get(SomeEntityClass.class)
                .where(SomeEntityClass::getName, mutatedName)
                .hasSize(1)
                .singleElement()
                .andExpected(entity -> Objects.equals(entity.getName(), mutatedName))
                .andExpected(entity -> Objects.equals(entity.getDescription(), "seed description"))
                .assertEntity();
    }

    @Test
    @DisplayName("Given object body when creating Mongo entity then object is persisted")
    void givenObjectBody_whenCreatingMongoEntity_thenObjectIsPersisted() {
        final String entityName = "entity-body-" + UUID.randomUUID();

        this.forgeIt.mongo()
                .create(SomeEntityClass.class)
                .body(SomeEntityClass.builder()
                        .name(entityName)
                        .description("created-from-object")
                        .build());

        this.forgeIt.mongo()
                .get(SomeEntityClass.class)
                .where(SomeEntityClass::getName, entityName)
                .hasSize(1)
                .singleElement()
                .andExpected(entity -> Objects.equals(entity.getDescription(), "created-from-object"))
                .assertEntity();
    }

    @Test
    @DisplayName("Given object body when mutating Mongo entity then mutation is stored")
    void givenObjectBody_whenMutatingMongoEntity_thenMutationIsStored() {
        final String sourceName = "source-" + UUID.randomUUID();
        final String mutatedName = "mutated-" + UUID.randomUUID();

        this.forgeIt.mongo()
                .create(SomeEntityClass.class)
                .body(SomeEntityClass.builder()
                        .name(sourceName)
                        .description("object-source")
                        .build())
                .mutate(entity -> entity.setName(mutatedName));

        this.forgeIt.mongo()
                .get(SomeEntityClass.class)
                .where(SomeEntityClass::getName, mutatedName)
                .hasSize(1)
                .singleElement()
                .andExpected(entity -> Objects.equals(entity.getName(), mutatedName))
                .andExpected(entity -> Objects.equals(entity.getDescription(), "object-source"))
                .assertEntity();
    }
}
