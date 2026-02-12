package com.sitionix.forgeit.mongodb.internal.domain;

import com.sitionix.forgeit.application.validator.EntityJsonComparator;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertions;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.loader.JsonLoader;
import com.sitionix.forgeit.mongodb.internal.config.MongoProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public final class MongoEntityAssertions implements DbEntityAssertions {

    private final JsonLoader jsonLoader;
    private final MongoProperties properties;

    public MongoEntityAssertions(final JsonLoader jsonLoader,
                                 final MongoProperties properties) {
        this.jsonLoader = jsonLoader;
        this.properties = properties;
    }

    @Override
    public <E> void assertEntityMatchesJson(final DbEntityHandle<E> handle,
                                            final String jsonResourceName,
                                            final String... fieldsToIgnore) {
        final String expectedJson = this.loadExpectedJson(handle, jsonResourceName);
        final Set<String> ignoreFields = this.buildIgnoreFields(handle, fieldsToIgnore);
        EntityJsonComparator.assertMatchesJson(handle.get(), expectedJson, ignoreFields);
    }

    @Override
    public <E> void assertEntityMatchesJsonStrict(final DbEntityHandle<E> handle,
                                                  final String jsonResourceName,
                                                  final String... fieldsToIgnore) {
        final String expectedJson = this.loadExpectedJson(handle, jsonResourceName);
        final Set<String> ignoreFields = this.buildIgnoreFields(handle, fieldsToIgnore);
        EntityJsonComparator.assertMatchesJsonStrict(handle.get(), expectedJson, ignoreFields);
    }

    private <E> String loadExpectedJson(final DbEntityHandle<E> handle,
                                        final String jsonResourceName) {
        if (!StringUtils.hasText(jsonResourceName)) {
            throw new IllegalArgumentException("Json resource name must not be blank");
        }
        if (handle == null || handle.get() == null) {
            throw new AssertionError(String.format("Entity handle is empty for json '%s'", jsonResourceName));
        }
        final String basePath = this.resolveExpectedEntityPath();
        this.jsonLoader.setBasePath(basePath);
        return this.jsonLoader.getFromFile(jsonResourceName);
    }

    private String resolveExpectedEntityPath() {
        if (this.properties.getPaths() == null || this.properties.getPaths().getEntity() == null) {
            throw new IllegalStateException("forge-it.modules.mongodb.paths.entity must be configured");
        }
        if (StringUtils.hasText(this.properties.getPaths().getEntity().getExpected())) {
            return this.properties.getPaths().getEntity().getExpected().trim();
        }
        if (StringUtils.hasText(this.properties.getPaths().getEntity().getCustom())) {
            return this.properties.getPaths().getEntity().getCustom().trim();
        }
        throw new IllegalStateException("forge-it.modules.mongodb.paths.entity.expected must be configured");
    }

    private Set<String> buildIgnoreFields(final DbEntityHandle<?> handle,
                                          final String... fieldsToIgnore) {
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
