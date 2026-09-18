package com.sitionix.forgeit.consumer.e2e;

import com.sitionix.forgeit.consumer.auth.endpoint.MockMvcEndpoint;
import com.sitionix.forgeit.core.test.E2E;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Opt-in live test; its name is deliberately outside the default Surefire includes. */
@E2E
class AuthE2E {
    @Autowired private E2eSupport forgeIt;

    @Test void givenUserLoginRequest_whenLogin_thenReturnDefaultLogin() {
        this.forgeIt.mockMvc(ServiceContracts.AUTH).ping(MockMvcEndpoint.loginDefault()).assertDefault();
    }
}
