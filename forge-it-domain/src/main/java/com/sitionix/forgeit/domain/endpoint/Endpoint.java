package com.sitionix.forgeit.domain.endpoint;

import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;
import com.sitionix.forgeit.domain.endpoint.wiremock.WiremockDefault;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.BiFunction;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public final class Endpoint<Req, Res> {

    private UrlBuilder urlBuilder;
    private HttpMethod method;
    private Class<Req> requestClass;
    private Class<Res> responseClass;
    private WiremockDefault wireMockDefault;
    private MockmvcDefault mockmvcDefault;

    public static <Req, Res> Endpoint<Req, Res> createContract(
            final String urlTemplate,
            final HttpMethod method,
            final Class<Req> requestClass,
            final Class<Res> responseClass,
            final WiremockDefault wireMockDefault
    ) {
        return new Endpoint<>(new UrlBuilder(urlTemplate),
                method,
                requestClass,
                responseClass,
                wireMockDefault,
                null);
    }

    public static <Req, Res> Endpoint<Req, Res> createContract(
            final String urlTemplate,
            final HttpMethod method,
            final Class<Req> requestClass,
            final Class<Res> responseClass
    ) {
        return new Endpoint<>(new UrlBuilder(urlTemplate),
                method,
                requestClass,
                responseClass,
                null,
                null);
    }

    public static <Req, Res> Endpoint<Req, Res> createContract(
            final String urlTemplate,
            final HttpMethod method,
            final Class<Req> requestClass,
            final Class<Res> responseClass,
            final MockmvcDefault mockmvcDefault
    ) {
        return new Endpoint<>(new UrlBuilder(urlTemplate),
                method,
                requestClass,
                responseClass,
                null,
                mockmvcDefault);
    }

    @RequiredArgsConstructor
    public static class UrlBuilder {

        @Getter
        private final String template;

        private String resolved;

        @Getter
        private Map<String, ?> queryParameters;

        @Getter
        private Map<String, ?> pathParameters;

        public void applyQueryParameters(final Map<String, ?> parameters,
                                    final BiFunction<String, Map<String, ?>, String> resolver) {
            if (parameters == null || parameters.isEmpty()) {
                return;
            }
            this.queryParameters = parameters;
            final String basePath = this.resolved != null ? this.resolved : this.template;
            this.resolved = resolver.apply(basePath, parameters);
        }
        public void applyParameters(final Map<String, ?> parameters,
                                    final BiFunction<String, Map<String, ?>, String> resolver) {
            if (parameters == null || parameters.isEmpty()) {
                return;
            }
            this.pathParameters = parameters;
            this.resolved = resolver.apply(this.template, parameters);
        }

        public String getUrl() {
            return this.resolved != null ? this.resolved : this.template;
        }

        public boolean hasQueryParameters() {
            return this.queryParameters != null && !this.queryParameters.isEmpty();
        }
    }
}
