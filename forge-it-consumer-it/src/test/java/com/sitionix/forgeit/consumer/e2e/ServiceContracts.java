package com.sitionix.forgeit.consumer.e2e;

import com.sitionix.forgeit.domain.endpoint.ServiceContract;

public final class ServiceContracts {
    public static final ServiceContract AUTH = ServiceContract.builder()
            .baseUrlFromProperty("consumer.http.auth-base-url").build();
    private ServiceContracts() { }
}
