package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.db.contract.EndpointContract;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class PostgresqlIT {

    @Autowired
    private ForgeItSupport forgeIt;

    @Test
    void test() {
        forgeIt.mockMvc()
                .ping(EndpointContract.USER_CREATE)
                .assertDefault();
    }
}
