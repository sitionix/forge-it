package com.sitionix.forgeit.application.validator;

import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertions;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import com.sitionix.forgeit.domain.model.sql.RelationalModuleProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(RelationalFeatureMarker.class)
public class RelationalEntityAssertions implements DbEntityAssertions {

    private final ObjectProvider<JsonLoader> jsonLoaderProvider;
    private final RelationalModuleProperties properties;

    @Override
    public <E> void assertEntityMatchesJson(final DbEntityHandle<E> handle,
                                            final String jsonResourceName,
                                            final String... fieldsToIgnore) {
        this.assertEntityMatchesJson(handle,
                jsonResourceName,
                this.properties.getPaths().getEntity().getCustom(),
                fieldsToIgnore);
    }

    @Override
    public <E> void assertEntityMatchesJsonStrict(final DbEntityHandle<E> handle,
                                                  final String jsonResourceName,
                                                  final String... fieldsToIgnore) {
        this.assertEntityMatchesJsonStrict(handle,
                jsonResourceName,
                this.properties.getPaths().getEntity().getCustom(),
                fieldsToIgnore);
    }

    private <E> void assertEntityMatchesJson(final DbEntityHandle<E> handle,
                                             final String jsonResourceName,
                                             final String basePath,
                                             final String... fieldsToIgnore) {
        if (jsonResourceName == null) {
            throw new IllegalArgumentException("Json resource name must not be null");
        }
        if (handle == null || handle.get() == null) {
            throw new AssertionError(String.format("Entity handle is empty for json '%s'", jsonResourceName));
        }
        if (basePath == null) {
            throw new IllegalStateException("Relational entity fixture path is not configured");
        }

        final JsonLoader loader = this.jsonLoaderProvider.getIfAvailable();
        if (loader == null) {
            throw new IllegalStateException("JsonLoader bean is not available; ensure forge-it-application is on the classpath");
        }

        loader.setBasePath(basePath);
        final String expectedJson = loader.getFromFile(jsonResourceName);
        final Set<String> ignoreFields = buildIgnoreFields(handle, fieldsToIgnore);
        EntityJsonComparator.assertMatchesJson(handle.get(), expectedJson, ignoreFields);
    }

    private <E> void assertEntityMatchesJsonStrict(final DbEntityHandle<E> handle,
                                                   final String jsonResourceName,
                                                   final String basePath,
                                                   final String... fieldsToIgnore) {
        if (jsonResourceName == null) {
            throw new IllegalArgumentException("Json resource name must not be null");
        }
        if (handle == null || handle.get() == null) {
            throw new AssertionError(String.format("Entity handle is empty for json '%s'", jsonResourceName));
        }
        if (basePath == null) {
            throw new IllegalStateException("Relational entity fixture path is not configured");
        }

        final JsonLoader loader = this.jsonLoaderProvider.getIfAvailable();
        if (loader == null) {
            throw new IllegalStateException("JsonLoader bean is not available; ensure forge-it-application is on the classpath");
        }

        loader.setBasePath(basePath);
        final String expectedJson = loader.getFromFile(jsonResourceName);
        final Set<String> ignoreFields = buildIgnoreFields(handle, fieldsToIgnore);
        EntityJsonComparator.assertMatchesJsonStrict(handle.get(), expectedJson, ignoreFields);
    }

    private Set<String> buildIgnoreFields(final DbEntityHandle<?> handle, final String... fieldsToIgnore) {
        final Set<String> ignoreFields = new LinkedHashSet<>();
        if (handle != null && handle.contract() != null) {
            ignoreFields.addAll(handle.contract().fieldsToIgnoreOnMatch());
        }
        if (fieldsToIgnore != null) {
            for (final String field : fieldsToIgnore) {
                if (field != null) {
                    ignoreFields.add(field);
                }
            }
        }
        return ignoreFields;
    }
}
