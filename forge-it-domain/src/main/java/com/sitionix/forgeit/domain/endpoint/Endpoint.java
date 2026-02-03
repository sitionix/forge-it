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
        return createContract(urlTemplate,
                method,
                requestClass,
                responseClass,
                mockmvcDefault,
                null);
    }

    public static <Req, Res> Endpoint<Req, Res> createContract(
            final String urlTemplate,
            final HttpMethod method,
            final Class<Req> requestClass,
            final Class<Res> responseClass,
            final String defaultToken
    ) {
        return createContract(urlTemplate,
                method,
                requestClass,
                responseClass,
                null,
                defaultToken);
    }

    public static <Req, Res> Endpoint<Req, Res> createContract(
            final String urlTemplate,
            final HttpMethod method,
            final Class<Req> requestClass,
            final Class<Res> responseClass,
            final MockmvcDefault mockmvcDefault,
            final String defaultToken
    ) {
        return new Endpoint<>(new UrlBuilder(urlTemplate),
                method,
                requestClass,
                responseClass,
                null,
                mergeMockmvcDefaults(mockmvcDefault, defaultToken));
    }

    private static MockmvcDefault mergeMockmvcDefaults(final MockmvcDefault mockmvcDefault,
                                                       final String defaultToken) {
        if (mockmvcDefault == null && defaultToken == null) {
            return null;
        }
        return context -> {
            if (mockmvcDefault != null) {
                mockmvcDefault.applyDefaults(context);
            }
            if (defaultToken != null) {
                context.token(defaultToken);
            }
        };
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
            this.resolved = resolver.apply(this.template, parameters);
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
