package com.sitionix.forgeit.wiremock.internal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.sitionix.forgeit.wiremock.api.Parameter;
import java.util.LinkedHashMap;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FindRequestPattern {

    private String method;
    private String url;
    private String urlPattern;
    private String urlPath;
    private String urlPathTemplate;

    private Map<String, WireMockValuePattern> queryParameters;
    private Map<String, WireMockValuePattern> pathParameters;

    public static FindRequestPattern findByUrlPatternAndPathParams(final Endpoint<?, ?> endpoint) {
        return FindRequestPattern.builder()
                .method(endpoint.getMethod().name())
                .urlPathTemplate(endpoint.getUrlBuilder().getTemplate())
                .pathParameters(toParameters(endpoint.getUrlBuilder().getPathParameters()))
                .build();
    }

    public static FindRequestPattern findByUrlPathAndQuery(final Endpoint<?, ?> endpoint) {
        final Map<String, ?> pathParameters = endpoint.getUrlBuilder().getPathParameters();
        final boolean hasPathParameters = pathParameters != null && !pathParameters.isEmpty();
        return FindRequestPattern.builder()
                .urlPathTemplate(hasPathParameters ? endpoint.getUrlBuilder().getTemplate() : null)
                .pathParameters(hasPathParameters ? toParameters(pathParameters) : null)
                .urlPath(hasPathParameters ? null : stripQuery(endpoint.getUrlBuilder().getUrl()))
                .method(endpoint.getMethod().name())
                .queryParameters(toParameters(endpoint.getUrlBuilder().getQueryParameters()))
                .build();
    }

    private static Map<String, WireMockValuePattern> toParameters(final Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        final Map<String, WireMockValuePattern> parameters = new LinkedHashMap<>();
        for (final Map.Entry<String, ?> entry : source.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            parameters.put(entry.getKey(), toPattern(entry.getValue()));
        }
        return parameters;
    }

    private static WireMockValuePattern toPattern(final Object value) {
        if (value instanceof Parameter parameter) {
            final StringValuePattern pattern = parameter.toPattern();
            return WireMockValuePattern.from(pattern);
        }
        return WireMockValuePattern.equalTo(String.valueOf(value));
    }

    private static String stripQuery(final String url) {
        if (url == null) {
            return null;
        }
        final int queryIndex = url.indexOf('?');
        return queryIndex >= 0 ? url.substring(0, queryIndex) : url;
    }
}
