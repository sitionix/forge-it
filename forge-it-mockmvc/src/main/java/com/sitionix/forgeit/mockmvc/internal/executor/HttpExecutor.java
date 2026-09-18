package com.sitionix.forgeit.mockmvc.internal.executor;

import org.springframework.web.util.UriUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public final class HttpExecutor implements MockMvcExecutor {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^/}]+)}");
    private final HttpClient client;
    private final URI baseUrl;
    private final Duration timeout;

    public HttpExecutor(HttpClient client, URI baseUrl, Duration timeout) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.timeout = requireTimeout(timeout, "request-timeout");
    }

    public static Duration requireTimeout(Duration value, String name) {
        if (value == null || value.compareTo(Duration.ofMillis(1)) < 0 || value.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalArgumentException(name + " must be between 1ms and 10m");
        }
        return value;
    }

    @Override
    public MockMvcResponse execute(MockMvcRequest request) {
        final HttpRequest httpRequest = this.buildRequest(request);
        // The future deadline bounds the entire response, including a stalled body.
        var future = this.client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            var response = future.get(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
            return new MockMvcResponse(response.statusCode(), response.body());
        } catch (InterruptedException ex) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw failure(request, "interrupted");
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw failure(request, "request timeout");
        } catch (ExecutionException ex) {
            // Exception messages can contain URLs/secrets; retain only the cause category.
            throw failure(request, ex.getCause().getClass().getSimpleName());
        }
    }

    private HttpRequest buildRequest(MockMvcRequest request) {
        try {
            final URI uri = this.requestUri(request);
            final HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(this.timeout);
            if (request.body() != null) { builder.header("Content-Type", "application/json"); }
            request.headers().forEach((name, value) -> { if (value != null) builder.header(name, value); });
            var cookies = new ArrayList<String>();
            request.cookies().forEach((name, value) -> { if (value != null) cookies.add(name + "=" + value); });
            if (!cookies.isEmpty()) { builder.header("Cookie", String.join("; ", cookies)); }
            builder.method(request.method().name(), request.body() == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8));
            return builder.build();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid HTTP " + request.method()
                    + " request configuration: check the endpoint path, parameters, headers and cookies");
        }
    }

    private IllegalStateException failure(MockMvcRequest request, String reason) {
        return new IllegalStateException("HTTP " + request.method() + " to " + this.baseUrl.getScheme()
                + "://" + this.baseUrl.getHost() + (this.baseUrl.getPort() < 0 ? "" : ":" + this.baseUrl.getPort())
                + " failed: " + reason);
    }

    private URI requestUri(MockMvcRequest request) {
        final String template = request.pathTemplate();
        if (template == null || !template.startsWith("/") || template.startsWith("//")
                || template.contains("?") || template.contains("#") || template.contains("\\")) {
            throw new IllegalArgumentException("HTTP endpoint must be an origin-relative path without query or fragment");
        }
        var matcher = PLACEHOLDER.matcher(template);
        var path = new StringBuilder();
        int end = 0;
        while (matcher.find()) {
            path.append(encodePath(template.substring(end, matcher.start())));
            Object value = request.pathParameters().get(matcher.group(1));
            if (value == null) { throw new IllegalArgumentException("Missing endpoint path parameter: " + matcher.group(1)); }
            path.append(UriUtils.encode(String.valueOf(value), StandardCharsets.UTF_8));
            end = matcher.end();
        }
        path.append(encodePath(template.substring(end)));
        String basePath = this.baseUrl.getRawPath();
        if (basePath.endsWith("/")) { basePath = basePath.substring(0, basePath.length() - 1); }
        var query = new ArrayList<String>();
        request.queryParameters().forEach((key, values) -> values.forEach(value -> query.add(
                UriUtils.encode(key, StandardCharsets.UTF_8) + "=" + UriUtils.encode(value, StandardCharsets.UTF_8))));
        return URI.create(this.baseUrl.getScheme() + "://" + this.baseUrl.getRawAuthority() + basePath + path
                + (query.isEmpty() ? "" : "?" + String.join("&", query)));
    }

    /** Preserve existing percent escapes in the endpoint; encode raw Unicode and spaces exactly once. */
    private static String encodePath(String path) {
        var result = new StringBuilder();
        int start = 0;
        for (int i = 0; i + 2 < path.length(); i++) {
            if (path.charAt(i) == '%' && Character.digit(path.charAt(i + 1), 16) >= 0
                    && Character.digit(path.charAt(i + 2), 16) >= 0) {
                result.append(UriUtils.encodePath(path.substring(start, i), StandardCharsets.UTF_8));
                result.append(path, i, i + 3);
                i += 2;
                start = i + 1;
            }
        }
        return result.append(UriUtils.encodePath(path.substring(start), StandardCharsets.UTF_8)).toString();
    }
}
