package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.db.contract.DbContracts;
import com.sitionix.forgeit.consumer.db.contract.EndpointContract;
import com.sitionix.forgeit.consumer.db.entity.CategoryEntity;
import com.sitionix.forgeit.consumer.db.entity.ProductEntity;
import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import com.sitionix.forgeit.consumer.db.entity.UserStatusEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
class PostgresqlIT {

    @Autowired
    private ForgeItSupport forgeIt;

    @Test
    @DisplayName("Given user with products when asserting strict json then matches")
    void givenUserWithProducts_whenAssertingStrictJson_thenMatches() {
        final DbGraphResult result = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json")
                        .label("user"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("first_product_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("second_product_entity.json"))
                .build();

        this.forgeIt.postgresql()
                .assertEntity(result.entity(DbContracts.USER_ENTITY_DB_CONTRACT, "user"))
                .withJson("custom_user_with_products_entity.json")
                .ignoreFields("id")
                .withFetchedRelations()
                .assertMatchesStrict();

        this.forgeIt.postgresql()
                .assertEntities(DbContracts.USER_ENTITY_DB_CONTRACT)
                .withFetchedRelations()
                .containsAllWithJsons("custom_user_with_products_entity.json");

        this.forgeIt.postgresql()
                .assertEntities(UserEntity.class)
                .withFetchedRelations()
                .ignoreFields("id")
                .containsWithJsonsStrict("custom_user_with_products_entity.json");
    }

    @Test
    @DisplayName("Given user with status and products when created via json contracts then products persist")
    void givenUserWithStatusAndProducts_whenCreatedViaJsonContracts_thenPersistProducts() {
        final DbGraphResult result = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json")
                        .addChild(DbContracts.PRODUCT_ENTITY_DB_CONTRACT
                                .withJson("first_product_entity.json")
                                .label("first-product")
                                .addChild(DbContracts.CATEGORY_ENTITY_DB_CONTRACT
                                        .withJson("first_category_entity.json")
                                        .label("first-category")))
                        .addChild(DbContracts.PRODUCT_ENTITY_DB_CONTRACT
                                .withJson("second_product_entity.json")
                                .label("second-product")
                                .addChild(DbContracts.CATEGORY_ENTITY_DB_CONTRACT
                                        .withJson("second_category_entity.json")
                                        .label("second-category"))))
                .build();

        final ProductEntity firstProduct = result.entity(DbContracts.PRODUCT_ENTITY_DB_CONTRACT, "first-product")
                .update(product -> product.setDescription("Updated first"))
                .get();

        final ProductEntity secondProduct = result.entity(DbContracts.PRODUCT_ENTITY_DB_CONTRACT, "second-product")
                .update(product -> product.setDescription("Updated second"))
                .get();

        final CategoryEntity firstCategory = result.entity(DbContracts.CATEGORY_ENTITY_DB_CONTRACT, "first-category")
                .get();

        final CategoryEntity secondCategory = result.entity(DbContracts.CATEGORY_ENTITY_DB_CONTRACT, "second-category")
                .get();

        assertThat(firstProduct.getDescription()).isEqualTo("Updated first");
        assertThat(secondProduct.getDescription()).isEqualTo("Updated second");
        assertThat(firstProduct.getCategory()).isEqualTo(firstCategory);
        assertThat(secondProduct.getCategory()).isEqualTo(secondCategory);
    }

    @Test
    @DisplayName("Given multiple product invocations when entityAt called then order is preserved")
    void givenMultipleProductInvocations_whenEntityAtCalled_thenOrderIsPreserved() {
        final DbGraphResult result = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT
                        .withJson("first_product_entity.json")
                        .addChild(DbContracts.CATEGORY_ENTITY_DB_CONTRACT
                                .withJson("first_category_entity.json")))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT
                        .withJson("second_product_entity.json")
                        .addChild(DbContracts.CATEGORY_ENTITY_DB_CONTRACT
                                .withJson("second_category_entity.json")))
                .build();

        this.forgeIt.postgresql()
                .assertEntity(result.entityAt(DbContracts.PRODUCT_ENTITY_DB_CONTRACT, 0))
                .withJson("first_product_entity.json")
                .assertMatches();

        this.forgeIt.postgresql()
                .assertEntity(result.entityAt(DbContracts.PRODUCT_ENTITY_DB_CONTRACT, 1))
                .withJson("second_product_entity.json")
                .assertMatches();
    }

    @Test
    @DisplayName("Given multiple products when asserting by contract then all fixtures match")
    void givenMultipleProducts_whenAssertingByContract_thenAllFixturesMatch() {
        this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("first_product_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("second_product_entity.json"))
                .build();

        this.forgeIt.postgresql()
                .assertEntities(DbContracts.PRODUCT_ENTITY_DB_CONTRACT)
                .containsAllWithJsons("first_product_entity.json", "second_product_entity.json");
    }

    @Test
    @DisplayName("Given multiple products when asserting by entity type then all fixtures match")
    void givenMultipleProducts_whenAssertingByEntityType_thenAllFixturesMatch() {
        this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("first_product_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("second_product_entity.json"))
                .build();

        this.forgeIt.postgresql()
                .assertEntities(ProductEntity.class)
                .containsAllWithJsons("first_product_entity.json", "second_product_entity.json");
    }

    @Test
    @DisplayName("Given multiple products when exact matching then counts align")
    void givenMultipleProducts_whenExactMatching_thenCountsAlign() {
        this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("first_product_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("second_product_entity.json"))
                .build();

        this.forgeIt.postgresql()
                .assertEntities(DbContracts.PRODUCT_ENTITY_DB_CONTRACT)
                .containsAllWithJsons("first_product_entity.json", "second_product_entity.json");
    }

    @Test
    @DisplayName("Given extra products when exact matching then assertion fails")
    void givenExtraProducts_whenExactMatching_thenAssertionFails() {
        this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("first_product_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("second_product_entity.json"))
                .build();

        assertThatThrownBy(() -> this.forgeIt.postgresql()
                .assertEntities(DbContracts.PRODUCT_ENTITY_DB_CONTRACT)
                .containsWithJsonsStrict("first_product_entity.json"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("Given mismatched fixture when exact matching then assertion fails")
    void givenMismatchedFixture_whenExactMatching_thenAssertionFails() {
        this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("first_product_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("second_product_entity.json"))
                .build();

        assertThatThrownBy(() -> this.forgeIt.postgresql()
                .assertEntities(DbContracts.PRODUCT_ENTITY_DB_CONTRACT)
                .containsWithJsonsStrict("first_product_entity.json", "first_product_entity.json"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("Given fixture missing product description when strict matching then assertion fails")
    void givenFixtureMissingProductDescription_whenStrictMatching_thenAssertionFails() {
        this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("first_product_entity.json"))
                .build();

        assertThatThrownBy(() -> this.forgeIt.postgresql()
                .assertEntities(DbContracts.PRODUCT_ENTITY_DB_CONTRACT)
                .ignoreFields("id", "user")
                .containsWithJsonsStrict("first_product_missing_description_entity.json"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("Given product fixture when strict assertion used then matches json")
    void givenProductFixture_whenStrictAssertionUsed_thenMatchesJson() {
        final DbGraphResult result = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT
                        .withJson("first_product_entity.json")
                        .addChild(DbContracts.CATEGORY_ENTITY_DB_CONTRACT
                                .withJson("first_category_entity.json")))
                .build();

        this.forgeIt.postgresql()
                .assertEntity(result.entityAt(DbContracts.PRODUCT_ENTITY_DB_CONTRACT, 0))
                .withJson("first_product_entity.json")
                .ignoreFields("id", "category", "user")
                .assertMatchesStrict();
    }

    @Test
    @DisplayName("Given products without categories when built then category is optional")
    void givenProductsWithoutCategories_whenBuilt_thenCategoryIsOptional() {
        final DbGraphResult result = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("first_product_entity.json"))
                .to(DbContracts.PRODUCT_ENTITY_DB_CONTRACT.withJson("second_product_entity.json"))
                .build();

        final ProductEntity first = result.entityAt(DbContracts.PRODUCT_ENTITY_DB_CONTRACT, 0).get();
        final ProductEntity second = result.entityAt(DbContracts.PRODUCT_ENTITY_DB_CONTRACT, 1).get();

        assertThat(first.getCategory()).isNull();
        assertThat(second.getCategory()).isNull();
    }

    @Test
    @DisplayName("Given nested user graph when using addChild then labels resolve the exact nodes")
    void givenNestedUserGraph_whenUsingAddChild_thenLabelsResolveExactNodes() {
        final DbGraphResult result = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json")
                        .label("user")
                        .addChild(DbContracts.PRODUCT_ENTITY_DB_CONTRACT
                                .withJson("first_product_entity.json")
                                .label("product-1")
                                .addChild(DbContracts.CATEGORY_ENTITY_DB_CONTRACT
                                        .withJson("first_category_entity.json")
                                        .label("category-1")))
                        .addChild(DbContracts.PRODUCT_ENTITY_DB_CONTRACT
                                .withJson("second_product_entity.json")
                                .label("product-2")
                                .addChild(DbContracts.CATEGORY_ENTITY_DB_CONTRACT
                                        .withJson("second_category_entity.json")
                                        .label("category-2"))))
                .build();

        final UserEntity user = result.entity(DbContracts.USER_ENTITY_DB_CONTRACT, "user").get();
        final ProductEntity productOne = result.entity(DbContracts.PRODUCT_ENTITY_DB_CONTRACT, "product-1").get();
        final ProductEntity productTwo = result.entity(DbContracts.PRODUCT_ENTITY_DB_CONTRACT, "product-2").get();
        final CategoryEntity categoryOne = result.entity(DbContracts.CATEGORY_ENTITY_DB_CONTRACT, "category-1").get();
        final CategoryEntity categoryTwo = result.entity(DbContracts.CATEGORY_ENTITY_DB_CONTRACT, "category-2").get();

        assertThat(user.getStatus().getDescription()).isEqualTo("INACTIVE");
        assertThat(productOne.getCategory()).isEqualTo(categoryOne);
        assertThat(productTwo.getCategory()).isEqualTo(categoryTwo);
    }

    @Test
    void givenOneCreatedRecord_whenCreateUser_thenVerifySize() {
        this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build();

        this.forgeIt.mockMvc()
                .ping(EndpointContract.USER_CREATE)
                .assertDefault();

        final List<UserEntity> entities = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getAll();

        assertThat(entities).hasSize(2);
    }

    @Test
    void shouldInitializeSchemaAndSeedStatuses() {
        final List<UserStatusEntity> statuses = this.forgeIt.postgresql()
                .get(UserStatusEntity.class)
                .getAll();

        assertThat(statuses)
                .hasSize(3)
                .extracting(UserStatusEntity::getDescription)
                .containsExactlyInAnyOrder("ACTIVE", "INACTIVE", "BLOCKED");
    }

    @Test
    void shouldCreateUserFromDefaultContractAndAttachStatus() {
        final DbGraphResult result = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build();

        final UserEntity created = result.entity(DbContracts.USER_ENTITY_DB_CONTRACT).get();

        assertThat(created.getId()).isNotNull();
        assertThat(created.getUsername()).isEqualTo("default_user");
        assertThat(created.getStatus().getDescription()).isEqualTo("ACTIVE");

        final List<UserEntity> persisted = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getAll();

        assertThat(persisted)
                .singleElement()
                .extracting(UserEntity::getUsername)
                .isEqualTo("default_user");
    }

    @Test
    void shouldCreateUserFromCustomJsonPayload() {
        final DbGraphResult result = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .build();

        final UserEntity created = result.entity(DbContracts.USER_ENTITY_DB_CONTRACT).get();

        assertThat(created.getUsername()).isEqualTo("custom_user");
        assertThat(created.getStatus().getDescription()).isEqualTo("INACTIVE");
    }

    @Test
    void shouldPersistProvidedEntityAndRetrieveById() {
        final UserEntity manualUser = UserEntity.builder()
                .username("manual_user")
                .build();

        final DbGraphResult result = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withEntity(manualUser))
                .build();

        final Long id = result.entity(DbContracts.USER_ENTITY_DB_CONTRACT).get().getId();

        final UserEntity persisted = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getById(id);

        assertThat(persisted.getUsername()).isEqualTo("manual_user");
        assertThat(persisted.getStatus().getDescription()).isEqualTo("BLOCKED");
    }

    @Test
    void shouldCreateUsersWithMixedPayloadsAndStatuses() {
        final DbGraphResult activeUser = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build();

        final DbGraphResult inactiveUser = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("custom_user_entity.json"))
                .build();

        final UserEntity blockedUser = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withEntity(UserEntity.builder()
                        .username("manual_batch_user")
                        .build()))
                .build()
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT)
                .get();

        final List<UserEntity> allUsers = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getAll();

        assertThat(allUsers)
                .hasSize(3)
                .extracting(UserEntity::getUsername, user -> user.getStatus().getDescription())
                .containsExactlyInAnyOrder(
                        tuple(activeUser.entity(DbContracts.USER_ENTITY_DB_CONTRACT).get().getUsername(), "ACTIVE"),
                        tuple(inactiveUser.entity(DbContracts.USER_ENTITY_DB_CONTRACT).get().getUsername(), "INACTIVE"),
                        tuple(blockedUser.getUsername(), "BLOCKED")
                );
    }

    @Test
    void shouldRejectDuplicateUsernamesAndKeepExistingRecord() {
        final UserEntity existingUser = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build()
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT)
                .get();

        assertThatThrownBy(() -> this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build())
                .isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);

        final List<UserEntity> users = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getAll();

        assertThat(users)
                .singleElement()
                .extracting(UserEntity::getUsername)
                .isEqualTo(existingUser.getUsername());
    }

    @Test
    void shouldRetrieveEachUserIndividuallyAfterSeparateGraphs() {
        final UserEntity activeUser = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build()
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT)
                .get();

        final UserEntity inactiveUser = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("priority_user_entity.json"))
                .build()
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT)
                .get();

        final UserEntity fetchedActive = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getById(activeUser.getId());

        final UserEntity fetchedInactive = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getById(inactiveUser.getId());

        assertThat(fetchedActive.getUsername()).isEqualTo(activeUser.getUsername());
        assertThat(fetchedActive.getStatus().getDescription()).isEqualTo("ACTIVE");

        assertThat(fetchedInactive.getUsername()).isEqualTo("priority_user");
        assertThat(fetchedInactive.getStatus().getDescription()).isEqualTo("INACTIVE");
    }
}
