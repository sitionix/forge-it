package com.sitionix.forgeit.wiremock.internal.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.wiremock.WiremockDefaultContext;
import com.sitionix.forgeit.domain.endpoint.HttpMethod;
import com.sitionix.forgeit.domain.endpoint.wiremock.WiremockDefault;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.wiremock.api.WireMockPathParams;
import com.sitionix.forgeit.wiremock.api.WireMockQueryParams;
import com.sitionix.forgeit.wiremock.internal.configs.PathTemplate;
import com.sitionix.forgeit.wiremock.internal.journal.WireMockJournal;
import com.sitionix.forgeit.wiremock.internal.loader.WireMockLoaderResources;
import java.util.Collections;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

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
    private Map<String, StringValuePattern> queryParameters;
    private Map<String, StringValuePattern> pathParameters;
    private Map<String, StringValuePattern> headerParameters;
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
        if (fileName == null || fileName.isBlank()) {
            return this;
        }

        this.requestJsonName = fileName;

        final JsonLoader loader = useDefault ? this.loaderResources.mappingDefaultRequest()
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

    public WireMockMappingBuilder<Req, Res> urlWithQueryParam(final WireMockQueryParams parameters) {
        if (parameters != null && !parameters.asMap().isEmpty()) {
            this.method = this.endpoint.getMethod();
            this.url = null;
            this.endpoint.getUrlBuilder().applyQueryParameters(parameters.asMap(), (template, vars) -> template);
            this.urlPath = this.endpoint.getUrlBuilder().getTemplate();
            this.queryParameters = this.toParameters(parameters.asMap());
        }
        return this;
    }

    public WireMockMappingBuilder<Req, Res> path(final WireMockPathParams parameters) {
        if (parameters != null && !parameters.asMap().isEmpty()) {
            this.method = this.endpoint.getMethod();
            this.urlPath = this.endpoint.getUrlBuilder().getUrl();
            this.pathParameters = this.toParameters(parameters.asMap());
        }
        return this;
    }

    public WireMockMappingBuilder<Req, Res> pathPattern(final WireMockPathParams parameters) {
        if (parameters != null && !parameters.asMap().isEmpty()) {
            this.method = this.endpoint.getMethod();
            this.pathParameters = this.toParameters(parameters.asMap());
            this.endpoint.getUrlBuilder().applyParameters(parameters.asMap(), PathTemplate::withPathParams);
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

    public WireMockMappingBuilder<Req, Res> header(final String name, final Object value) {
        if (name == null || name.isBlank() || value == null) {
            return this;
        }
        if (this.headerParameters == null) {
            this.headerParameters = new LinkedHashMap<>();
        }
        this.headerParameters.put(name, this.toPattern(value));
        return this;
    }

    private WireMockMappingBuilder<Req, Res> loadResponseJson(final String fileName,
                                                              final boolean useDefault,
                                                              final Consumer<Res> mutator) {
        if (fileName == null || fileName.isBlank()) {
            return this;
        }

        this.responseJsonName = fileName;

        final JsonLoader loader = useDefault ? this.loaderResources.mappingDefaultResponse()
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

        final WiremockDefault defaultsContext = this.endpoint.getWireMockDefault();
        if (nonNull(defaultsContext)) {
            this.endpoint.getWireMockDefault().applyDefaults(new MissingDefaultContext());
        }

        return this.create();
    }

    public WireMockMappingBuilder<Req, Res> applyDefault(final Consumer<DefaultContext> consumer) {
        if (consumer != null) {
            consumer.accept(this.defaultContext);
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
        final MappingBuilder mappingBuilder = WireMock.request(this.endpoint.getMethod().name(), requireUrlPattern());

        if (WireMockMappingBuilder.this.requestJson != null) {
            mappingBuilder.withRequestBody(WireMock.equalToJson(WireMockMappingBuilder.this.requestJson));
        }

        if (WireMockMappingBuilder.this.queryParameters != null) {
            WireMockMappingBuilder.this.queryParameters.forEach(mappingBuilder::withQueryParam);
        }

        if (WireMockMappingBuilder.this.pathParameters != null) {
            WireMockMappingBuilder.this.pathParameters.forEach(mappingBuilder::withPathParam);
        }

        if (WireMockMappingBuilder.this.headerParameters != null) {
            WireMockMappingBuilder.this.headerParameters.forEach(mappingBuilder::withHeader);
        }

        mappingBuilder.willReturn(this.buildResponseDefinition());

        return mappingBuilder;
    }

    private UrlPattern requireUrlPattern() {
        if (WireMockMappingBuilder.this.url != null) {
            return WireMock.urlEqualTo(WireMockMappingBuilder.this.url);
        }

        if (WireMockMappingBuilder.this.urlPathPattern != null) {
            return WireMock.urlPathTemplate(WireMockMappingBuilder.this.urlPathPattern);
        }

        if (WireMockMappingBuilder.this.urlPath != null) {
            return WireMock.urlPathEqualTo(WireMockMappingBuilder.this.urlPath);
        }

        throw new IllegalStateException("URL pattern must be specified");
    }

    private Map<String, StringValuePattern> toParameters(final Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, StringValuePattern> parameters = new LinkedHashMap<>();
        for (final Map.Entry<String, ?> entry : source.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            parameters.put(entry.getKey(), this.toPattern(entry.getValue()));
        }
        return parameters;
    }

    private StringValuePattern toPattern(final Object value) {
        if (value instanceof com.sitionix.forgeit.wiremock.api.Parameter parameter) {
            return parameter.toPattern();
        }
        if (value instanceof Parameter parameter) {
            return parameter.toPattern();
        }
        return WireMock.equalTo(String.valueOf(value));
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

    public final class DefaultContext implements WiremockDefaultContext {

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

        @Override
        public DefaultContext header(final String name, final Object value) {
            WireMockMappingBuilder.this.header(name, value);
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

    private final class MissingDefaultContext implements WiremockDefaultContext {

        @Override
        public WiremockDefaultContext matchesJson(final String json) {
            if (WireMockMappingBuilder.this.requestJson == null) {
                WireMockMappingBuilder.this.defaultMatchesJson(json);
            }
            return this;
        }

        @Override
        public WiremockDefaultContext responseBody(final String json) {
            if (WireMockMappingBuilder.this.responseJson == null) {
                WireMockMappingBuilder.this.defaultResponseBody(json);
            }
            return this;
        }

        @Override
        public WiremockDefaultContext responseStatus(final int status) {
            if (WireMockMappingBuilder.this.responseStatus == null) {
                WireMockMappingBuilder.this.responseStatus(HttpStatus.resolve(status));
            }
            return this;
        }

        @Override
        public WiremockDefaultContext plainUrl() {
            if (WireMockMappingBuilder.this.url == null
                    && WireMockMappingBuilder.this.urlPath == null
                    && WireMockMappingBuilder.this.urlPathPattern == null) {
                WireMockMappingBuilder.this.plainUrl();
            }
            return this;
        }

        @Override
        public WiremockDefaultContext header(final String name, final Object value) {
            if (WireMockMappingBuilder.this.headerParameters == null
                    || !WireMockMappingBuilder.this.headerParameters.containsKey(name)) {
                WireMockMappingBuilder.this.header(name, value);
            }
            return this;
        }
    }
}
