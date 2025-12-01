package com.sitionix.forgeit.wiremock.internal.configs;

import lombok.AllArgsConstructor;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.regex.Pattern;

@AllArgsConstructor
public class PathTemplate {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^/}]+)}");

    public static String withPathParams(final String template, final Map<String, ?> vars) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }

        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath(template);

        final UriComponents components = (vars == null || vars.isEmpty())
                ? builder.build()
                : builder.buildAndExpand(vars);

        final String resolvedPath = components.toUriString();

        if (PLACEHOLDER.matcher(resolvedPath).find()) {
            throw new IllegalArgumentException("Not all placeholders were resolved in the template: " + template);
        }
        return resolvedPath;
    }

    public static String withQueryParams(final String template, final Map<String, ?> vars) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }

        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath(template);

        if (vars != null && !vars.isEmpty()) {
            vars.forEach((key, value) -> {
                if (value == null) {
                    return;
                }

                if (value instanceof Iterable<?> iterable) {
                    iterable.forEach(item -> builder.queryParam(key, item));
                } else if (value.getClass().isArray()) {
                    final Object[] array = (Object[]) value;
                    for (Object item : array) {
                        builder.queryParam(key, item);
                    }
                } else {
                    builder.queryParam(key, value);
                }
            });
        }

        return builder.build().toUriString();
    }

}
