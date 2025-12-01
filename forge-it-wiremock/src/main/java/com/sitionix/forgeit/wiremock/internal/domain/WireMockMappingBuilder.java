package com.sitionix.forgeit.wiremock.internal.domain;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.EndpointDefaultsContext;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.WireMockDefault;
import com.sitionix.forgeit.domain.loader.ResourcesLoader;
import com.sitionix.forgeit.wiremock.internal.configs.PathTemplate;
import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal;
import com.sitionix.forgeit.wiremock.internal.loader.WireMockLoaderResources;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static java.util.Objects.nonNull;

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
    private Map<String, Parameter> queryParameters;
    private Map<String, Parameter> pathParameters;
    private String url;
    private String urlPath;
    private String urlPathPattern;
    private HttpStatus responseStatus;
    private Long responseDelayMilliseconds;
    private Consumer<Req> defaultRequestMutator;
    private Consumer<Res> defaultResponseMutator;

    private final DefaultContext defaultContext;
    private final DefaultMutationContext<Req, Res> defaultMutationContext;

    private final WireMockJournal wireMockJournal;

    public WireMockMappingBuilder(final Endpoint<Req, Res> endpoint,
                                  final WireMockLoaderResources loaderResources,
                                  final ObjectMapper objectMapper,
                                  final WireMock wireMockClient,
                                  final WireMockJournal wireMockJournal) {
        this.endpoint = endpoint;
        this.loaderResources = loaderResources;
        this.objectMapper = objectMapper;
        this.wireMockClient = wireMockClient;
        this.defaultContext = new DefaultContext();
        this.defaultMutationContext = new DefaultMutationContext<>();
        this.wireMockJournal = wireMockJournal;
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
        this.url = this.endpoint.getUrlBuilder().getUrl();
        this.urlPath = null;
        this.urlPathPattern = null;
        return this;
    }

    public WireMockMappingBuilder<Req, Res> urlWithQueryParam(final Map<String, Parameter> parameters) {
        if (parameters != null) {
            this.method = this.endpoint.getMethod();
            this.url = null;
            this.endpoint.getUrlBuilder().applyParameters(parameters, PathTemplate::withQueryParams);
            this.urlPath = this.endpoint.getUrlBuilder().getTemplate();
            this.queryParameters = parameters;
        }
        return this;
    }

    public WireMockMappingBuilder<Req, Res> path(final Map<String, Parameter> parameters) {
        if (parameters != null) {
            this.method = this.endpoint.getMethod();
            this.urlPath = this.endpoint.getUrlBuilder().getUrl();
            this.pathParameters = parameters;
        }
        return this;
    }

    public WireMockMappingBuilder<Req, Res> pathPattern(final Map<String, Parameter> parameters) {
        if (parameters != null) {
            this.method = this.endpoint.getMethod();
            this.pathParameters = parameters;
            this.endpoint.getUrlBuilder().applyParameters(parameters, PathTemplate::withPathParams);
            this.urlPathPattern = this.endpoint.getUrlBuilder().getTemplate();
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

    private String writeValueAsString(final Object obj) {
        try {
            return this.objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    public WireMockMappingBuilder<Req, Res> delayForResponse(final long milliseconds) {
        this.responseDelayMilliseconds = milliseconds;
        return this;
    }

    public RequestBuilder<Req, Res> createDefault() {
        return this.createDefault(null);
    }

    public RequestBuilder<Req, Res> createDefault(final Consumer<DefaultMutationContext<Req, Res>> mutator) {
        if (mutator != null) {
            mutator.accept(this.defaultMutationContext);
        }

        this.endpoint.getWireMockDefault().applyDefaults(this.defaultContext);
        return this.create();
    }

    public WireMockMappingBuilder<Req, Res> applyDefault(final Consumer<DefaultContext> consumer) {
        if (consumer != null) {
            consumer.accept(this.defaultContext);
        }

        final WireMockDefault defaultsContext = this.endpoint.getWireMockDefault();
        if (nonNull(defaultsContext)) {
            defaultsContext.applyDefaults(this.defaultContext);
        }
        return this;
    }

    public RequestBuilder<Req, Res> create() {
        final MappingBuilder mappingBuilder = this.buildMappingBuilder();
        final StubMapping stubMapping = mappingBuilder.build();
        this.wireMockClient.register(stubMapping);

        return this.wireMockJournal.check(this.endpoint)
                .id(stubMapping.getId())
                .json(this.requestJson);
    }

    private MappingBuilder buildMappingBuilder() {
        final String requestMethod = requireMethod();
        final MappingBuilder mappingBuilder = WireMock.request(requestMethod, requireUrlPattern());

        if (WireMockMappingBuilder.this.requestJson != null) {
            mappingBuilder.withRequestBody(WireMock.equalToJson(WireMockMappingBuilder.this.requestJson));
        }

        if (WireMockMappingBuilder.this.queryParameters != null) {
            WireMockMappingBuilder.this.queryParameters.forEach((key, value) ->
                    mappingBuilder.withQueryParam(key, value.toPattern()));
        }

        if (WireMockMappingBuilder.this.pathParameters != null) {
            WireMockMappingBuilder.this.pathParameters.forEach((key, value) ->
                    mappingBuilder.withPathParam(key, value.toPattern()));
        }

        mappingBuilder.willReturn(buildResponseDefinition());

        return mappingBuilder;
    }

    private UrlPattern requireUrlPattern() {
        if (WireMockMappingBuilder.this.url != null) {
            return WireMock.urlEqualTo(WireMockMappingBuilder.this.url);
        }

        if (WireMockMappingBuilder.this.urlPath != null) {
            return WireMock.urlPathEqualTo(WireMockMappingBuilder.this.urlPath);
        }

        if (WireMockMappingBuilder.this.urlPathPattern != null) {
            return WireMock.urlPathTemplate(WireMockMappingBuilder.this.urlPathPattern);
        }

        throw new IllegalStateException("URL pattern must be specified");
    }

    private String requireMethod() {
        if (WireMockMappingBuilder.this.method == null) {
            throw new IllegalStateException("HTTP method must be specified");
        }
        return WireMockMappingBuilder.this.method.name();
    }

    private ResponseDefinitionBuilder buildResponseDefinition() {
        final ResponseDefinitionBuilder responseBuilder = WireMock.aResponse();

        if (WireMockMappingBuilder.this.responseStatus != null) {
            responseBuilder.withStatus(WireMockMappingBuilder.this.responseStatus.value());
        }

        if (this.responseJson != null) {
            responseBuilder
                    .withBody(this.responseJson)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }

        if (WireMockMappingBuilder.this.responseDelayMilliseconds != null) {
            responseBuilder.withFixedDelay(WireMockMappingBuilder.this.responseDelayMilliseconds.intValue());
        }

        return responseBuilder;
    }

    public final class DefaultContext implements EndpointDefaultsContext {

        @Override
        public DefaultContext matchesJson(final String json) {
            WireMockMappingBuilder.this.defaultMatchesJson(json);
            return this;
        }

        @Override
        public DefaultContext responseBody(final String json) {
            WireMockMappingBuilder.this.defaultResponseBody(json);
            return this;
        }

        @Override
        public DefaultContext responseStatus(final int status) {
            WireMockMappingBuilder.this.responseStatus(HttpStatus.resolve(status));
            return this;
        }

        @Override
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
}
