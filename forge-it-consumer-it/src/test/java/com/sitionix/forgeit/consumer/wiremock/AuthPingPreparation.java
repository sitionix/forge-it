package com.sitionix.forgeit.consumer.wiremock;

import com.sitionix.forgeit.consumer.ForgeItSupport;
import com.sitionix.forgeit.consumer.auth.endpoint.WireMockEndpoint;
import com.sitionix.forgeit.domain.preparation.DataPreparation;
import org.springframework.http.HttpStatus;

public class AuthPingPreparation implements DataPreparation<ForgeItSupport> {

    @Override
    public void prepare(final ForgeItSupport forgeit) {
        forgeit.wiremock()
                .createMapping(WireMockEndpoint.ping())
                .responseStatus(HttpStatus.NO_CONTENT)
                .plainUrl()
                .create();
    }
}
