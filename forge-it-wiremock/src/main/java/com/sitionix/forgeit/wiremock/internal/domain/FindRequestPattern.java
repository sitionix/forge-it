package com.sitionix.forgeit.wiremock.internal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import lombok.Builder;

import java.util.Collections;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FindRequestPattern {

    private String method;
    private String url;
    private String urlPattern;
    private String urlPath;

    private Map<String, MultiValuePattern> queryParameters;
    private Map<String, StringValuePattern> pathParameters;

    public static FindRequestPattern findByUrlPatternAndPathParams(final Endpoint<?, ?> endpoint) {
        return FindRequestPattern.builder()
                .method(endpoint.getMethod().name())
                .urlPattern(endpoint.getUrlBuilder().getTemplate())
                .pathParameters(toPathParameters(endpoint.getUrlBuilder().getPathParameters()))
                .build();
    }

    public static FindRequestPattern findByUrlPathAndQuery(final Endpoint<?, ?> endpoint) {
        return FindRequestPattern.builder()
                .urlPath(endpoint.getUrlBuilder().getTemplate())
                .method(endpoint.getMethod().name())
                .queryParameters(toQueryParameters(endpoint.getUrlBuilder().getQueryParameters()))
                .build();
    }

    private static Map<String, StringValuePattern> toPathParameters(final Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return source.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> toStringValuePattern(e.getValue())
                ));
    }

    private static Map<String, MultiValuePattern> toQueryParameters(final Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return source.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> MultiValuePattern.of(toStringValuePattern(e.getValue()))
                ));
    }

    private static StringValuePattern toStringValuePattern(final Object value) {
        if (value instanceof com.sitionix.forgeit.wiremock.api.Parameter parameter) {
            return parameter.toPattern();
        }
        if (value instanceof Parameter parameter) {
            return parameter.toPattern();
        }
        return WireMock.equalTo(String.valueOf(value));
    }
}
