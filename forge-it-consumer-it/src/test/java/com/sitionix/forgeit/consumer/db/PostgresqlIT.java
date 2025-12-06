package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.db.contract.DbContracts;
import com.sitionix.forgeit.consumer.db.contract.EndpointContract;
import com.sitionix.forgeit.consumer.db.entity.UserStatusEntity;
import com.sitionix.forgeit.consumer.db.jpa.PostgresJpaRepository;
import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import com.sitionix.forgeit.domain.contract.graph.DbGraphResult;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class PostgresqlIT {

    @Autowired
    private ForgeItSupport forgeIt;

    @Autowired
    private PostgresJpaRepository postgresJpaRepository;

    @Autowired
    private Environment environment;

    @BeforeEach
    void cleanDatabase() {
        this.postgresJpaRepository.deleteAll();
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
    void shouldExposeConnectionPropertiesFromContainerManager() {
        final String jdbcUrl = this.environment.getProperty("forge-it.postgresql.connection.jdbc-url");
        final Integer port = this.environment.getProperty("forge-it.postgresql.connection.port", Integer.class);
        final String host = this.environment.getProperty("forge-it.postgresql.connection.host");
        final String database = this.environment.getProperty("forge-it.postgresql.connection.database");
        final String username = this.environment.getProperty("forge-it.postgresql.connection.username");
        final String password = this.environment.getProperty("forge-it.postgresql.connection.password");

        assertThat(jdbcUrl).isNotBlank();
        assertThat(port).isNotNull().isPositive();
        assertThat(host).isNotBlank();
        assertThat(database).isEqualTo("forge-it");
        assertThat(username).isEqualTo("forge-it");
        assertThat(password).isEqualTo("forge-it-pwd");
        assertThat(jdbcUrl).contains(host).contains(String.valueOf(port));
    }
}
