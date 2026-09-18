package com.sitionix.forgeit.mockmvc.internal.executor;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** One instance per builder; MVC-specific matchers never touch the HTTP transport. */
public final class MvcExecutor implements MockMvcExecutor {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^/}]+)}");
    private final MockMvc mockMvc;
    private org.springframework.test.web.servlet.ResultActions resultActions;
    private final List<ResultMatcher> matchers = new ArrayList<>();

    public MvcExecutor(MockMvc mockMvc) { this.mockMvc = mockMvc; }
    public void addMatcher(ResultMatcher matcher) { this.matchers.add(matcher); }

    public void verifyMatchers() throws Exception {
        for (var matcher : this.matchers) { this.resultActions.andExpect(matcher); }
    }

    @Override
    public MockMvcResponse execute(MockMvcRequest request) throws Exception {
        String path = request.pathTemplate();
        if (!request.pathParameters().isEmpty()) {
            path = UriComponentsBuilder.fromPath(path).buildAndExpand(request.pathParameters()).toUriString();
        }
        if (PLACEHOLDER.matcher(path).find()) {
            throw new IllegalArgumentException("Path parameters are required for endpoint template");
        }
        final MockHttpServletRequestBuilder builder = switch (request.method()) {
            case GET -> get(path);
            case POST -> post(path);
            case PUT -> put(path);
            case PATCH -> patch(path);
            case DELETE -> delete(path);
            default -> throw new IllegalStateException("Unsupported HTTP method: " + request.method());
        };
        if (request.body() != null) { builder.contentType(MediaType.APPLICATION_JSON).content(request.body()); }
        request.queryParameters().forEach((key, values) -> builder.param(key, values.toArray(String[]::new)));
        request.headers().forEach((key, value) -> { if (value != null) builder.header(key, value); });
        request.cookies().forEach((key, value) -> { if (value != null) builder.cookie(new Cookie(key, value)); });
        final long started = System.nanoTime();
        this.resultActions = this.mockMvc.perform(builder);
        var result = this.resultActions.andReturn();
        if (result.getRequest().isAsyncStarted()) {
            this.resultActions = this.mockMvc.perform(asyncDispatch(result));
        }
        var response = this.resultActions.andReturn().getResponse();
        return new MockMvcResponse(response.getStatus(), response.getContentAsString(),
                Duration.ofNanos(System.nanoTime() - started));
    }
}
