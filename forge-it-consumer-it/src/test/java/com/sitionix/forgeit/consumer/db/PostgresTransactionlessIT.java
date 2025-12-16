package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.db.contract.DbContracts;
import com.sitionix.forgeit.consumer.db.entity.UserEntity;
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
class PostgresTransactionlessIT {

    @Autowired
    private ForgeItSupport forgeIt;

    @Test
    @Order(1)
    void shouldExecuteGraphWithoutSurroundingTransaction() {
        final UserEntity created = this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build()
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT);

        assertThat(created.getId()).isNotNull();

        final List<UserEntity> persisted = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getAll();

        assertThat(persisted)
                .hasSize(1)
                .first()
                .extracting(UserEntity::getUsername)
                .isEqualTo("default_user");
    }

    @Test
    @Order(2)
    void shouldLeaveDatabaseCleanBetweenTests() {
        final List<UserEntity> users = this.forgeIt.postgresql()
                .get(UserEntity.class)
                .getAll();

        assertThat(users).isEmpty();
    }
}
