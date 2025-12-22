package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.db.contract.DbContracts;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.domain.ForgeItConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@DisplayName("Given tx policy mandatory, when graph executed without transaction, then configuration error is raised")
@TestPropertySource(properties = "forge-it.modules.postgresql.tx-policy=MANDATORY")
class PostgresTxPolicyMandatoryIT {

    @Autowired
    private ForgeItSupport forgeIt;

    @Test
    @DisplayName("Given mandatory tx policy when execute graph then fails without @Transactional")
    void givenMandatoryTxPolicy_whenExecuteGraph_thenFailsWithoutTransactional() {
        assertThatThrownBy(() -> this.forgeIt.postgresql()
                .create()
                .to(DbContracts.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DbContracts.USER_ENTITY_DB_CONTRACT)
                .build())
                .isInstanceOf(ForgeItConfigurationException.class)
                .hasMessageContaining("GraphTxPolicy.MANDATORY");
    }
}
