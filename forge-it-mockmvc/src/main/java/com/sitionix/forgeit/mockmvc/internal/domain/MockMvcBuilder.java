package com.sitionix.forgeit.mockmvc.internal.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefaultContext;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.mockmvc.api.PathParams;
import com.sitionix.forgeit.mockmvc.api.QueryParams;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.sitionix.forgeit.mockmvc.internal.validator.CustomResultMatcher.jsonEqualsIgnore;
import static java.util.Objects.nonNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class MockMvcBuilder<Req, Res> {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^/}]+)}");

    private final MockMvc mockMvc;
    private final MockMvcLoader mockMvcLoader;
    private final Endpoint<Req, Res> endpoint;
    private final Class<Req> requestType;
    private final Class<Res> responseType;

    private final ObjectMapper objectMapper;
    private Consumer<Req> defaultRequestMutator;
    private Consumer<Res> defaultResponseMutator;
    private final List<ResultMatcher> extraMatchers;
    private final List<String> responseFieldsToIgnore;
    private String requestJson;
    private String responseJson;
    private Map<String, ?> queryParameters;
    private Map<String, ?> pathParameters;
    private String token;
    private String defaultToken;
    private boolean tokenProvided;
    private HttpStatus expectedStatus;
    private final DefaultContext defaultContext;

    private final DefaultMutationContext<Req, Res> defaultMutationContext;

    public MockMvcBuilder(final MockMvc mockMvc,
                          final MockMvcLoader mockMvcLoader,
                          final ObjectMapper objectMapper,
                          final Endpoint<Req, Res> endpoint) {
        this.mockMvc = mockMvc;
        this.endpoint = endpoint;
        this.mockMvcLoader = mockMvcLoader;
        this.objectMapper = objectMapper;
        this.requestType = endpoint.getRequestClass();
        this.responseType = endpoint.getResponseClass();
        this.defaultMutationContext = new DefaultMutationContext<>();
        this.defaultContext = new DefaultContext();
        this.extraMatchers = new ArrayList<>();
        this.responseFieldsToIgnore = new ArrayList<>();
    }

    public MockMvcBuilder<Req, Res> withRequest(final String requestName) {
        if (nonNull(requestName)) {
            this.loadRequest(requestName, null, false);
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> withRequest(final String requestName, final Consumer<Req> requestMutator) {
        if (nonNull(requestName)) {
            this.loadRequest(requestName, requestMutator, false);
        }
        return this;
    }

    private MockMvcBuilder<Req, Res> defaultRequest(final String requestName) {
        if (nonNull(requestName)) {
            this.loadRequest(requestName, null, true);
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> expectResponse(final String responseName, final Consumer<Res> responseMutator) {
        if (nonNull(responseName)) {
            this.loadResponse(responseName, responseMutator, false);
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> expectResponse(final String responseName, final String... fieldsToIgnore) {
        if (nonNull(responseName)) {
            this.responseFieldsToIgnore.addAll(List.of(fieldsToIgnore));
            this.loadResponse(responseName, null, false);
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> expectResponse(final String responseName, final Consumer<Res> responseMutator, final String... fieldsToIgnore) {
        if (nonNull(responseName)) {
            this.responseFieldsToIgnore.addAll(List.of(fieldsToIgnore));
            this.loadResponse(responseName, responseMutator, false);
        }
        return this;
    }

    private MockMvcBuilder<Req, Res> defaultResponse(final String responseName) {
        if (nonNull(responseName)) {
            this.loadResponse(responseName, null, true);
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> expectStatus(final HttpStatus status) {
        if (nonNull(status)) {
            this.expectedStatus = status;
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> andExpectPath(final ResultMatcher matcher) {
        if (nonNull(matcher)) {
            this.extraMatchers.add(matcher);
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> token(final String token) {
        this.token = token;
        this.tokenProvided = true;
        return this;
    }

    public MockMvcBuilder<Req, Res> withQueryParameters(final QueryParams parameters) {
        if (nonNull(parameters) && !parameters.asMap().isEmpty()) {
            this.queryParameters = parameters.asMap();
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> withPathParameters(final PathParams parameters) {
        if (nonNull(parameters) && !parameters.asMap().isEmpty()) {
            this.pathParameters = parameters.asMap();
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> applyDefault(final Consumer<DefaultContext> consumer) {
        if (nonNull(consumer)) {
            consumer.accept(this.defaultContext);
        }
        return this;
    }

    public void assertDefault() {
        this.assertDefault(null);
    }

    public void assertDefault(final Consumer<DefaultMutationContext<Req, Res>> mutator) {
        if (mutator != null) {
            mutator.accept(this.defaultMutationContext);
        }

        final MockmvcDefault defaultsContext = this.endpoint.getMockmvcDefault();
        if (nonNull(defaultsContext)) {
            defaultsContext.applyDefaults(new MissingDefaultContext());
        }

        this.assertAndCreate();
    }

    public void assertAndCreate() {
        try {
            final MockmvcDefault defaultsContext = this.endpoint.getMockmvcDefault();
            if (nonNull(defaultsContext)) {
                defaultsContext.applyDefaults(new MissingDefaultContext());
            }
            final MockHttpServletRequestBuilder httpRequest = this.buildHttpRequest();
            final String resolvedToken = this.resolveToken();
            if (nonNull(resolvedToken)) {
                httpRequest.header(HttpHeaders.AUTHORIZATION, resolvedToken);
            }
            final var mvcResultActions = this.mockMvc.perform(httpRequest);
            if (nonNull(this.responseJson)) {
                mvcResultActions.andExpect(jsonEqualsIgnore(this.responseJson, this.responseFieldsToIgnore.toArray(new String[0])));
            }
            if (nonNull(this.expectedStatus)) {
                mvcResultActions.andExpect(MockMvcResultMatchers.status().is(this.expectedStatus.value()));
            }
            for (final ResultMatcher matcher : this.extraMatchers) {
                mvcResultActions.andExpect(matcher);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to createAndAssert MockMvc request", e);
        }
    }

    private void loadRequest(final String jsonName, final Consumer<Req> mutator, final boolean isDefault) {
        if (jsonName == null || jsonName.isBlank()) {
            return;
        }

        final JsonLoader loader = isDefault ? this.mockMvcLoader.mvcDefaultRequest() :
                this.mockMvcLoader.mvcRequest();

        final Consumer<Req> effectiveMutator = isDefault
                ? mutator != null ? mutator : this.defaultRequestMutator
                : mutator;

        if (effectiveMutator != null) {
            final Req requestObject = loader.getFromFile(jsonName, this.endpoint.getRequestClass());
            effectiveMutator.accept(requestObject);
            this.requestJson = this.writeValueAsString(requestObject);
        } else {
            this.requestJson = loader.getFromFile(jsonName);
        }

    }

    private String writeValueAsString(final Object obj) {
        try {
            return this.objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    private void loadResponse(final String jsonName, final Consumer<Res> mutator, final boolean isDefault) {
        if (jsonName == null || jsonName.isBlank()) {
            return;
        }

        final JsonLoader loader = isDefault ? this.mockMvcLoader.mvcDefaultResponse()
                : this.mockMvcLoader.mvcResponse();

        final Consumer<Res> effectiveMutator = isDefault
                ? mutator != null ? mutator : this.defaultResponseMutator
                : mutator;

        if (effectiveMutator != null) {
            final Res responseObject = loader.getFromFile(jsonName, this.endpoint.getResponseClass());
            effectiveMutator.accept(responseObject);
            this.responseJson = this.writeValueAsString(responseObject);
        } else {
            this.responseJson = loader.getFromFile(jsonName);
        }

    }

    private MockHttpServletRequestBuilder buildHttpRequest() {
        final String path = this.resolvePath();
        final MockHttpServletRequestBuilder builder = switch (this.endpoint.getMethod()) {
            case GET -> get(path);
            case POST -> post(path);
            case PUT -> put(path);
            case PATCH -> patch(path);
            case DELETE -> delete(path);
            default -> throw new IllegalStateException("Unsupported HTTP method: " + this.endpoint.getMethod());
        };
        if (nonNull(this.requestJson)) {
            builder.contentType(MediaType.APPLICATION_JSON).content(this.requestJson);
        }
        this.applyQueryParameters(builder);
        return builder;
    }

    private String resolvePath() {
        final String template = this.endpoint.getUrlBuilder().getTemplate();
        if (this.pathParameters == null || this.pathParameters.isEmpty()) {
            if (PLACEHOLDER.matcher(template).find()) {
                throw new IllegalArgumentException("Path parameters are required for template: " + template);
            }
            return template;
        }

        final String resolvedPath = UriComponentsBuilder.fromPath(template)
                .buildAndExpand(this.pathParameters)
                .toUriString();

        if (PLACEHOLDER.matcher(resolvedPath).find()) {
            throw new IllegalArgumentException("Not all placeholders were resolved in the template: " + template);
        }
        return resolvedPath;
    }

    private void applyQueryParameters(final MockHttpServletRequestBuilder builder) {
        if (this.queryParameters == null || this.queryParameters.isEmpty()) {
            return;
        }
        this.queryParameters.forEach((key, value) -> this.applyQueryParameter(builder, key, value));
    }

    private void applyQueryParameter(final MockHttpServletRequestBuilder builder,
                                     final String key,
                                     final Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (final Object item : iterable) {
                this.applyQueryParameter(builder, key, item);
            }
            return;
        }
        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                this.applyQueryParameter(builder, key, Array.get(value, i));
            }
            return;
        }
        builder.param(key, String.valueOf(value));
    }

    private String resolveToken() {
        if (this.tokenProvided) {
            return StringUtils.hasText(this.token) ? this.token : null;
        }
        return this.defaultToken;
    }

    private void setDefaultToken(final String token) {
        this.defaultToken = token;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public final class DefaultContext implements MockmvcDefaultContext {

        @Override
        public DefaultContext withRequest(final String json) {
            MockMvcBuilder.this.defaultRequest(json);
            return this;
        }

        @Override
        public DefaultContext expectResponse(final String json) {
            MockMvcBuilder.this.defaultResponse(json);
            return this;
        }

        @Override
        public DefaultContext expectStatus(final int status) {
            MockMvcBuilder.this.expectStatus(HttpStatus.resolve(status));
            return this;
        }

        @Override
        public DefaultContext token(final String token) {
            MockMvcBuilder.this.setDefaultToken(token);
            return this;
        }
    }

    private final class MissingDefaultContext implements MockmvcDefaultContext {
        @Override
        public MockmvcDefaultContext withRequest(final String json) {
            if (MockMvcBuilder.this.requestJson == null) {
                MockMvcBuilder.this.defaultRequest(json);
            }
            return this;
        }

        @Override
        public MockmvcDefaultContext expectResponse(final String json) {
            if (MockMvcBuilder.this.responseJson == null) {
                MockMvcBuilder.this.defaultResponse(json);
            }
            return this;
        }

        @Override
        public MockmvcDefaultContext expectStatus(final int status) {
            if (MockMvcBuilder.this.expectedStatus == null) {
                MockMvcBuilder.this.expectStatus(HttpStatus.resolve(status));
            }
            return this;
        }

        @Override
        public MockmvcDefaultContext token(final String token) {
            if (MockMvcBuilder.this.defaultToken == null) {
                MockMvcBuilder.this.setDefaultToken(token);
            }
            return this;
        }
    }

    private final class TokenOnlyDefaultContext implements MockmvcDefaultContext {
        @Override
        public MockmvcDefaultContext withRequest(final String json) {
            return this;
        }

        @Override
        public MockmvcDefaultContext expectResponse(final String json) {
            return this;
        }

        @Override
        public MockmvcDefaultContext expectStatus(final int status) {
            return this;
        }

        @Override
        public MockmvcDefaultContext token(final String token) {
            if (MockMvcBuilder.this.defaultToken == null) {
                MockMvcBuilder.this.setDefaultToken(token);
            }
            return this;
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public final class DefaultMutationContext<R, T> {
        public DefaultMutationContext<R, T> mutateRequest(final Consumer<Req> mutator) {
            if (nonNull(mutator)) {
                MockMvcBuilder.this.defaultRequestMutator = mutator;
            }
            return this;
        }

        public DefaultMutationContext<R, T> mutateResponse(final Consumer<Res> mutator) {
            if (nonNull(mutator)) {
                MockMvcBuilder.this.defaultResponseMutator = mutator;
            }
            return this;
        }
    }
}
