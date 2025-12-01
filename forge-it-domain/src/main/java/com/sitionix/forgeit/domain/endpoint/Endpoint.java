package com.sitionix.forgeit.domain.endpoint;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.BiFunction;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Endpoint<Req, Res> {

    private final UrlBuilder urlBuilder;
    private final HttpMethod method;
    private final Class<Req> requestClass;
    private final Class<Res> responseClass;

    public static <Req, Res> Endpoint<Req, Res> createContract(
            final String urlTemplate,
            final HttpMethod method,
            final Class<Req> requestClass,
            final Class<Res> responseClass
    ) {
        return new Endpoint<>(new UrlBuilder(urlTemplate), method, requestClass, responseClass);
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
