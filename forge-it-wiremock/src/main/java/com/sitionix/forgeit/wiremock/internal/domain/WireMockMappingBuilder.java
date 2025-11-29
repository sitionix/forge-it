package com.sitionix.forgeit.wiremock.internal.domain;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import com.sitionix.forgeit.wiremock.internal.loader.WireMockLoaderResources;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WireMockMappingBuilder<Req, Res> {

    private final Endpoint<Req, Res> endpoint;
    private final WireMockLoaderResources loaderResources;
    private final ObjectMapper objectMapper;
    private final WireMock wireMockClient;

    private HttpMethod method;
    private String requestJsonName;
    private String requestJson;
    private String responseJsonName;
    private String responseJson;
    private Map<String, String> queryParameters;
    private Map<String, Object> pathParameters;
    private String url;
    private String urlPath;
    private String urlPathPattern;
    private HttpStatus responseStatus;
    private Long responseDelayMilliseconds;
    private Consumer<Req> defaultRequestMutator;
    private Consumer<Res> defaultResponseMutator;

    private final DefaultContext defaultContext;
    private final DefaultMutationContext<Req, Res> defaultMutationContext;

    public WireMockMappingBuilder(final Endpoint<Req, Res> endpoint,
            final WireMockLoaderResources loaderResources,
            final ObjectMapper objectMapper,
            final WireMock wireMockClient) {
        this.endpoint = endpoint;
        this.loaderResources = loaderResources;
        this.objectMapper = objectMapper;
        this.wireMockClient = wireMockClient;
        this.defaultContext = new DefaultContext();
        this.defaultMutationContext = new DefaultMutationContext<>();
    }

    public WireMockMappingBuilder<Req, Res> matchesJson(final String requestFileName) {
        return this.loadRequestJson(requestFileName, false, null);
    }

    public WireMockMappingBuilder<Req, Res> matchesJson(final String requestFileName, final Consumer<Req> mutator) {
        return this.loadRequestJson(requestFileName, false, mutator);
    }

    private WireMockMappingBuilder<Req, Res> defaultMatchesJson(final String requestFileName) {
        return this.loadRequestJson(requestFileName, true, null);
    }

    private WireMockMappingBuilder<Req, Res> loadRequestJson(final String fileName,
            final boolean useDefault,
            final Consumer<Req> mutator) {
        if (fileName == null) {
            return this;
        }

        this.requestJsonName = fileName;

        final ResourcesLoader loader = useDefault ? this.loaderResources.mappingDefaultRequest()
                : this.loaderResources.mappingRequest();

        final Consumer<Req> effectiveMutator = useDefault
                ? mutator != null ? mutator : this.defaultRequestMutator
                : mutator;

        if (effectiveMutator != null) {
            final Req requestObject = loader.getFromFile(fileName, this.endpoint.getRequestClass());
            effectiveMutator.accept(requestObject);
            this.requestJson = this.writeValueAsString(requestObject);
        } else {
            this.requestJson = loader.getFromFile(fileName);
        }

        return this;
    }

    public WireMockMappingBuilder<Req, Res> plainUrl() {
        this.method = this.endpoint.getMethod();
        this.url = this.endpoint.getUrl();
        this.urlPath = null;
        this.urlPathPattern = null;
        return this;
    }

    public WireMockMappingBuilder<Req, Res> urlWithQueryParam(final Map<String, String> parameters) {
        if (parameters != null) {
            this.method = this.endpoint.getMethod();
            this.url = this.endpoint.getUrl();
            this.queryParameters = parameters;
        }
        return this;
    }

    public WireMockMappingBuilder<Req, Res> path(final Map<String, String> parameters) {
        if (parameters != null) {
            this.method = this.endpoint.getMethod();
            this.urlPath = this.endpoint.getUrl();
            this.queryParameters = parameters;
        }
        return this;
    }

    public WireMockMappingBuilder<Req, Res> pathPattern(final Map<String, Object> parameters) {
        if (parameters != null) {
            this.method = this.endpoint.getMethod();
            this.pathParameters = parameters;
            this.urlPathPattern = this.endpoint.getUrl();
        }
        return this;
    }

    public WireMockMappingBuilder<Req, Res> responseStatus(final HttpStatus status) {
        this.responseStatus = status;
        return this;
    }

    public WireMockMappingBuilder<Req, Res> responseBody(final String responseFileName) {
        return this.loadResponseJson(responseFileName, false, null);
    }

    private WireMockMappingBuilder<Req, Res> defaultResponseBody(final String responseFileName) {
        return this.loadResponseJson(responseFileName, true, null);
    }

    public WireMockMappingBuilder<Req, Res> responseBody(final String responseFileName,
            final Consumer<Res> mutator) {
        return this.loadResponseJson(responseFileName, false, mutator);
    }

    private WireMockMappingBuilder<Req, Res> loadResponseJson(final String fileName,
            final boolean useDefault,
            final Consumer<Res> mutator) {
        if (fileName == null) {
            return this;
        }

        this.responseJsonName = fileName;

        final ResourcesLoader loader = useDefault ? this.loaderResources.mappingDefaultResponse()
                : this.loaderResources.mappingResponse();

        final Consumer<Res> effectiveMutator = useDefault
                ? mutator != null ? mutator : this.defaultResponseMutator
                : mutator;

        if (effectiveMutator != null) {
            final Res responseObject = loader.getFromFile(fileName, this.endpoint.getResponseClass());
            effectiveMutator.accept(responseObject);
            this.responseJson = this.writeValueAsString(responseObject);
        } else {
            this.responseJson = loader.getFromFile(fileName);
        }

        return this;
    }

    public WireMockMappingBuilder<Req, Res> delayForResponse(final long milliseconds) {
        this.responseDelayMilliseconds = milliseconds;
        return this;
    }

    public RequestBuilder createDefault() {
        return this.createDefault(null);
    }

    public RequestBuilder createDefault(final Consumer<DefaultMutationContext<Req, Res>> mutator) {
        if (mutator != null) {
            mutator.accept(this.defaultMutationContext);
        }
        return new RequestBuilder();
    }

    public WireMockMappingBuilder<Req, Res> applyDefault(final Consumer<DefaultContext> consumer) {
        if (consumer != null) {
            consumer.accept(this.defaultContext);
        }
        return this;
    }

    public RequestBuilder create() {
        return new RequestBuilder();
    }

    private String writeValueAsString(final Object object) {
        try {
            return this.objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert object to json", e);
        }
    }

    public final class DefaultContext {

        public DefaultContext matchesJson(final String json) {
            WireMockMappingBuilder.this.defaultMatchesJson(json);
            return this;
        }

        public DefaultContext responseBody(final String json) {
            WireMockMappingBuilder.this.defaultResponseBody(json);
            return this;
        }

        public DefaultContext responseStatus(final HttpStatus status) {
            WireMockMappingBuilder.this.responseStatus(status);
            return this;
        }

        public DefaultContext plainUrl() {
            WireMockMappingBuilder.this.plainUrl();
            return this;
        }
    }

    public final class DefaultMutationContext<R, T> {

        public DefaultMutationContext<R, T> mutateRequest(final Consumer<Req> mutator) {
            if (mutator != null) {
                WireMockMappingBuilder.this.defaultRequestMutator = mutator;
            }
            return this;
        }

        public DefaultMutationContext<R, T> mutateResponse(final Consumer<Res> mutator) {
            if (mutator != null) {
                WireMockMappingBuilder.this.defaultResponseMutator = mutator;
            }
            return this;
        }
    }

    public final class RequestBuilder {

        public RequestBuilder() {
        }

        public com.github.tomakehurst.wiremock.stubbing.StubMapping build() {
            final MappingBuilder mappingBuilder = this.buildMappingBuilder();
            WireMockMappingBuilder.this.wireMockClient.register(mappingBuilder);
            return mappingBuilder.build();
        }

        private MappingBuilder buildMappingBuilder() {
            final String requestMethod = requireMethod();
            final MappingBuilder mappingBuilder = WireMock.request(requestMethod, requireUrlPattern());

            if (WireMockMappingBuilder.this.requestJson != null) {
                mappingBuilder.withRequestBody(WireMock.equalToJson(WireMockMappingBuilder.this.requestJson));
            }

            if (WireMockMappingBuilder.this.queryParameters != null) {
                WireMockMappingBuilder.this.queryParameters.forEach((key, value) ->
                        mappingBuilder.withQueryParam(key, WireMock.equalTo(value)));
            }

            if (WireMockMappingBuilder.this.pathParameters != null) {
                WireMockMappingBuilder.this.pathParameters.forEach((key, value) ->
                        mappingBuilder.withPathParam(key, WireMock.equalTo(String.valueOf(value))));
            }

            mappingBuilder.willReturn(buildResponseDefinition());

            return mappingBuilder;
        }

        private ResponseDefinitionBuilder buildResponseDefinition() {
            final ResponseDefinitionBuilder responseBuilder = WireMock.aResponse();

            if (WireMockMappingBuilder.this.responseStatus != null) {
                responseBuilder.withStatus(WireMockMappingBuilder.this.responseStatus.value());
            }

            if (WireMockMappingBuilder.this.responseJson != null) {
                responseBuilder.withBody(WireMockMappingBuilder.this.responseJson);
            }

            if (WireMockMappingBuilder.this.responseDelayMilliseconds != null) {
                responseBuilder.withFixedDelay(WireMockMappingBuilder.this.responseDelayMilliseconds.intValue());
            }

            return responseBuilder;
        }

        private String requireMethod() {
            if (WireMockMappingBuilder.this.method == null) {
                throw new IllegalStateException("HTTP method must be specified");
            }
            return WireMockMappingBuilder.this.method.name();
        }

        private UrlPattern requireUrlPattern() {
            if (WireMockMappingBuilder.this.url != null) {
                return WireMock.urlEqualTo(WireMockMappingBuilder.this.url);
            }

            if (WireMockMappingBuilder.this.urlPath != null) {
                return WireMock.urlPathEqualTo(WireMockMappingBuilder.this.urlPath);
            }

            if (WireMockMappingBuilder.this.urlPathPattern != null) {
                return WireMock.urlPathMatching(WireMockMappingBuilder.this.urlPathPattern);
            }

            throw new IllegalStateException("URL pattern must be specified");
        }
    }
}
