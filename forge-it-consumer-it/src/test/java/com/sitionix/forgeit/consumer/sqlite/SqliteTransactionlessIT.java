package com.sitionix.forgeit.consumer.sqlite;

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
class SqliteTransactionlessIT {

    @Autowired
    private SqliteForgeItSupport forgeIt;

    @Test
    @Order(1)
    void shouldExecuteGraphWithoutSurroundingTransaction() {
        final UserEntity created = this.forgeIt.sqlite()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build()
                .entity(DbContracts.USER_ENTITY_DB_CONTRACT)
                .get();

        assertThat(created.getId()).isNotNull();

        final List<UserEntity> persisted = this.forgeIt.sqlite()
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
        final List<UserEntity> users = this.forgeIt.sqlite()
                .get(UserEntity.class)
                .getAll();

        assertThat(users).isEmpty();
    }
}
