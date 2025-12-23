package com.sitionix.forgeit.application.validator;

import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertionBuilder;
import com.sitionix.forgeit.domain.contract.assertion.DbEntityAssertions;
import com.sitionix.forgeit.domain.contract.graph.DbEntityHandle;
import com.sitionix.forgeit.domain.model.sql.DbEntityFetcher;
import org.springframework.beans.BeanWrapperImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for asserting a single entity against a JSON fixture.
 */
public final class EntityAssertionBuilder<E> implements DbEntityAssertionBuilder<E> {
    private final DbEntityHandle<E> handle;
    private final DbEntityAssertions entityAssertions;
    private final DbEntityFetcher entityFetcher;
    private final List<String> fieldsToIgnore;
    private String jsonResourceName;
    private boolean deepStructure;

    public EntityAssertionBuilder(final DbEntityHandle<E> handle,
                                  final DbEntityAssertions entityAssertions,
                                  final DbEntityFetcher entityFetcher) {
        this.handle = handle;
        this.entityAssertions = entityAssertions;
        this.entityFetcher = entityFetcher;
        this.fieldsToIgnore = new ArrayList<>();
    }

    /**
     * Select a JSON fixture name from the custom entity fixtures path.
     */
    @Override
    public EntityAssertionBuilder<E> withJson(final String jsonResourceName) {
        this.jsonResourceName = jsonResourceName;
        return this;
    }

    @Override
    public EntityAssertionBuilder<E> ignoreFields(final String... fields) {
        if (fields != null) {
            for (final String field : fields) {
                if (field != null) {
                    this.fieldsToIgnore.add(field);
                }
            }
        }
        return this;
    }

    /**
     * Reload the entity from the database by id before comparison.
     */
    @Override
    public EntityAssertionBuilder<E> withDeepStructure() {
        this.deepStructure = true;
        return this;
    }

    /**
     * Alias for {@link #withDeepStructure()}.
     */
    @Override
    public EntityAssertionBuilder<E> withFetchedRelations() {
        return this.withDeepStructure();
    }

    /**
     * Resolve and return the effective handle for debugging or inspection.
     */
    @Override
    public DbEntityHandle<E> actualHandle() {
        return this.resolveHandle();
    }

    /**
     * Compare only the fields present in the JSON fixture.
     */
    @Override
    public void assertMatches() {
        final DbEntityHandle<E> effectiveHandle = this.resolveHandle();
        this.entityAssertions.assertEntityMatchesJson(
                effectiveHandle,
                this.jsonResourceName,
                this.fieldsToIgnore.toArray(new String[0])
        );
    }

    /**
     * Compare the full JSON structure after removing ignored fields.
     */
    @Override
    public void assertMatchesStrict() {
        final DbEntityHandle<E> effectiveHandle = this.resolveHandle();
        this.entityAssertions.assertEntityMatchesJsonStrict(
                effectiveHandle,
                this.jsonResourceName,
                this.fieldsToIgnore.toArray(new String[0])
        );
    }

    private DbEntityHandle<E> resolveHandle() {
        if (!this.deepStructure) {
            return this.handle;
        }
        if (this.handle.contract() == null) {
            throw new IllegalStateException("Deep structure requires a contract-backed handle");
        }
        final Object id = new BeanWrapperImpl(this.handle.get())
                .getPropertyValue("id");
        if (id == null) {
            throw new IllegalStateException("Deep structure requires a non-null id");
        }
        final E reloaded = this.entityFetcher.reloadByIdWithRelations(this.handle.contract(), id);
        if (reloaded == null) {
            throw new IllegalStateException("Deep structure reload returned null");
        }
        return new DbEntityHandle<>(reloaded, this.handle.contract());
    }
}
