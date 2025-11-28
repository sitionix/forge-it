package com.sitionix.forgeit.domain.endpoint;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Endpoint<Req, Res> {

    private final String url;
    private final HttpMethod method;
    private final Class<Req> requestClass;
    private final Class<Res> responseClass;

    public static <Req, Res> Endpoint<Req, Res> createContract(final String url,
            final HttpMethod method,
            final Class<Req> requestClass,
            final Class<Res> responseClass
    ) {
        return new Endpoint<>(url, method, requestClass, responseClass);
    }
}

