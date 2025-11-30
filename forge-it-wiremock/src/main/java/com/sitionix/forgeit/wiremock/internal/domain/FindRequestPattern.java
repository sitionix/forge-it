package com.sitionix.forgeit.wiremock.internal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FindRequestPattern(String method, String urlPattern) {
    public static FindRequestPattern findPostByUrl(final HttpMethod method, final String urlPattern) {
        return new FindRequestPattern(method.name(), urlPattern);
    }
}
