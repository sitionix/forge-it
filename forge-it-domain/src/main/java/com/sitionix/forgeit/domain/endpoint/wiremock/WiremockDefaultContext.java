package com.sitionix.forgeit.domain.endpoint.wiremock;

public interface WiremockDefaultContext {

    WiremockDefaultContext matchesJson(String json);

    WiremockDefaultContext responseBody(String json);

    WiremockDefaultContext responseStatus(int status);

    WiremockDefaultContext plainUrl();
}
