package com.sitionix.forgeit.domain.endpoint;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Endpoint<Req, Res> {

    private final Class<Req> requestClass;
    private final Class<Res> responseClass;

    public static <Req, Res> Endpoint<Req, Res> createContract(final String url,
            HttpMethod method,
            final Class<Req> requestClass,
            final Class<Res> responseClass
    ) {
        return new Endpoint<>(requestClass, responseClass);
    }
}

