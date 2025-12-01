package com.sitionix.forgeit.wiremock.internal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import java.util.Collections;
import lombok.Builder;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FindRequestPattern {

    private String method;
    private String url;
    private String urlPattern;
    private String urlPath;

    private Map<String, Parameter> queryParameters;
    private Map<String, Parameter> pathParameters;

    public static FindRequestPattern findByUrlPatternAndPathParams(final Endpoint<?, ?> endpoint) {
        return FindRequestPattern.builder()
                .method(endpoint.getMethod().name())
                .urlPattern(endpoint.getUrlBuilder().getTemplate())
                .pathParameters(toParameters(endpoint.getUrlBuilder().getPathParameters()))
                .build();
    }

    public static FindRequestPattern findByUrlPathAndQuery(final Endpoint<?, ?> endpoint) {
        return FindRequestPattern.builder()
                .urlPath(endpoint.getUrlBuilder().getTemplate())
                .method(endpoint.getMethod().name())
                .queryParameters(toParameters(endpoint.getUrlBuilder().getQueryParameters()))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Parameter> toParameters(final Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }

        final Object firstValue = source.values().stream().findFirst().orElse(null);
        if (firstValue instanceof Parameter) {
            return (Map<String, Parameter>) source;
        }
        return source.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> Parameter.equalTo(String.valueOf(e.getValue()))
                ));
    }
}
