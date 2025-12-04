package com.sitionix.forgeit.mockmvc.internal.domain;

import static com.sitionix.forgeit.mockmvc.internal.validator.CustomResultMatcher.jsonEqualsIgnore;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefault;
import com.sitionix.forgeit.domain.endpoint.mockmvc.MockmvcDefaultContext;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.mockmvc.internal.loader.MockMvcLoader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class MockMvcBuilder<Req, Res> {
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
    private String token;
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

    public MockMvcBuilder<Req, Res> request(final String requestName) {
        if (nonNull(requestName)) {
            this.loadRequest(requestName, null, false);
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> request(final String requestName, final Consumer<Req> requestMutator) {
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

    public MockMvcBuilder<Req, Res> response(final String responseName, final Consumer<Res> responseMutator) {
        if (nonNull(responseName)) {
            this.loadResponse(responseName, responseMutator, false);
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> response(final String responseName, final String... fieldsToIgnore) {
        if (nonNull(responseName)) {
            this.responseFieldsToIgnore.addAll(List.of(fieldsToIgnore));
            this.loadResponse(responseName, null, false);
        }
        return this;
    }

    public MockMvcBuilder<Req, Res> response(final String responseName, final Consumer<Res> responseMutator, final String... fieldsToIgnore) {
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

    public MockMvcBuilder<Req, Res> status(final HttpStatus status) {
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
            this.endpoint.getMockmvcDefault().applyDefaults(this.defaultContext);
        }

        this.createAndAssert();
    }

    public void createAndAssert() {
        try {
            final MockHttpServletRequestBuilder httpRequest = this.buildHttpRequest();
            if (nonNull(this.token)) {
                httpRequest.header(HttpHeaders.AUTHORIZATION, this.token);
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
        if (jsonName == null) {
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
        if (jsonName == null) {
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
        final String path = this.endpoint.getUrlBuilder().getTemplate();
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
        return builder;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public final class DefaultContext implements MockmvcDefaultContext {

        @Override
        public DefaultContext request(final String json) {
            MockMvcBuilder.this.defaultRequest(json);
            return this;
        }

        @Override
        public DefaultContext response(final String json) {
            MockMvcBuilder.this.defaultResponse(json);
            return this;
        }

        @Override
        public DefaultContext status(final int status) {
            MockMvcBuilder.this.status(HttpStatus.resolve(status));
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
