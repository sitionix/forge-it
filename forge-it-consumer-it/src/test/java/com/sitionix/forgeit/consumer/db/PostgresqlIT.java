package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.db.contract.DbContracts;
import com.sitionix.forgeit.consumer.db.contract.EndpointContract;
import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import com.sitionix.forgeit.consumer.db.entity.UserStatusEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import org.hibernate.exception.ConstraintViolationException;
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

        final UserEntity created = result.entity(DbContracts.USER_ENTITY_DB_CONTRACT);

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

        final UserEntity created = result.entity(DbContracts.USER_ENTITY_DB_CONTRACT);

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

        final Long id = result.entity(DbContracts.USER_ENTITY_DB_CONTRACT).getId();

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
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT);

        final List<UserEntity> allUsers = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getAll();

        assertThat(allUsers)
                .hasSize(3)
                .extracting(UserEntity::getUsername, user -> user.getStatus().getDescription())
                .containsExactlyInAnyOrder(
                        tuple(activeUser.entity(DbContracts.USER_ENTITY_DB_CONTRACT).getUsername(), "ACTIVE"),
                        tuple(inactiveUser.entity(DbContracts.USER_ENTITY_DB_CONTRACT).getUsername(), "INACTIVE"),
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
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT);

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
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT);

        final UserEntity inactiveUser = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT.withJson("priority_user_entity.json"))
                .build()
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT);

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
