package com.sitionix.forgeit.wiremock.internal.domain;

import com.sitionix.forgeit.domain.endpoint.Endpoint;
import java.util.List;
import java.util.UUID;

public record WireMockCheck <Req, Res>(
        Endpoint<Req, Res> endpoint,
        String expectedJson,
        int atLeastTimes,
        List<String> ignoredFields,
        UUID id
) {
}
