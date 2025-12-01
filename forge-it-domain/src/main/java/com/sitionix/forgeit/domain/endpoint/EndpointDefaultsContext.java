package com.sitionix.forgeit.domain.endpoint;

public interface EndpointDefaultsContext {

    EndpointDefaultsContext matchesJson(String json);

    EndpointDefaultsContext responseBody(String json);

    EndpointDefaultsContext responseStatus(int status);

    EndpointDefaultsContext plainUrl();
}
