package com.sitionix.forgeit.application.loader.sql;

import com.sitionix.forgeit.domain.loader.SqlLoader;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import com.sitionix.forgeit.domain.model.sql.ScriptPhase;
import com.sitionix.forgeit.domain.model.sql.SqlScriptDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
@ConditionalOnBean(RelationalFeatureMarker.class)
public class SqlLoaderImpl implements SqlLoader {

    private static final String BASE_FORGE_IT_PATH = "forge-it";

    private final ResourcePatternResolver resolver;
    private String rootLocation;

    @Override
    public void setBasePath(final String basePath) {
        if (basePath == null || basePath.isBlank()) {
            throw new IllegalArgumentException("Base path for SQL scripts must not be null or blank");
        }

        final String normalized = basePath.endsWith("/")
                ? basePath.substring(0, basePath.length() - 1)
                : basePath;

        this.rootLocation = BASE_FORGE_IT_PATH + normalized;
    }

    @Override
    public List<SqlScriptDescriptor> loadOrderedScripts() {
        if (this.rootLocation == null) {
            throw new IllegalStateException("Base path for SQL scripts is not configured. Call setBasePath(...) first.");
        }

        final String pattern = "classpath*:" + this.rootLocation + "/**/*.sql";

        try {
            final Resource[] resources = this.resolver.getResources(pattern);

            return Arrays.stream(resources)
                    .map(this::toDescriptor)
                    .sorted(Comparator
                            .comparing((SqlScriptDescriptor d) -> d.phase().getOrder())
                            .thenComparing(SqlScriptDescriptor::order)
                            .thenComparing(SqlScriptDescriptor::path))
                    .toList();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load SQL scripts from " + this.rootLocation, e);
        }
    }

    private SqlScriptDescriptor toDescriptor(final Resource resource) {
        final String fullPath = this.safePath(resource);
        final String filename = Objects.requireNonNull(resource.getFilename());
        final ScriptPhase phase = this.resolvePhase(fullPath);
        final int order = this.extractNumericPrefix(filename);

        return new SqlScriptDescriptor(phase, order, fullPath);
    }

    private String safePath(final Resource resource) {
        try {
            return resource.getURL().toString();
        } catch (final IOException e) {
            return resource.getDescription();
        }
    }

    private ScriptPhase resolvePhase(final String path) {
        final String lower = path.toLowerCase();
        if (lower.contains("/schema/")) {
            return ScriptPhase.SCHEMA;
        }
        if (lower.contains("/constraints/")) {
            return ScriptPhase.CONSTRAINTS;
        }
        if (lower.contains("/data/")) {
            return ScriptPhase.DATA;
        }
        return ScriptPhase.CUSTOM;
    }

    private int extractNumericPrefix(final String filename) {
        int i = 0;
        while (i < filename.length() && Character.isDigit(filename.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return 10_000;
        }
        return Integer.parseInt(filename.substring(0, i));
    }
}
