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
class SqliteCleanupSmokeIT {

    @Autowired
    private SqliteForgeItSupport forgeIt;

    @Test
    @Order(1)
    void step1_shouldCreateUser() {
        final List<UserEntity> before = this.forgeIt.sqlite()
                .get(UserEntity.class)
                .getAll();

        assertThat(before).isEmpty();

        this.forgeIt.sqlite()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build();

        final List<UserEntity> userEntities = this.forgeIt.sqlite()
                .get(UserEntity.class)
                .getAll();

        assertThat(userEntities).hasSize(1);
    }

    @Test
    @Order(2)
    void step2_shouldSeeEmptyTableIfCleanupWorks() {
        final List<UserEntity> userEntities = this.forgeIt.sqlite()
                .get(UserEntity.class)
                .getAll();

        assertThat(userEntities).isEmpty();
    }
}
