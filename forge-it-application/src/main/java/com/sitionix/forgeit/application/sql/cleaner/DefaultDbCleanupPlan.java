package com.sitionix.forgeit.application.sql.cleaner;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.clean.CleanupPhase;
import com.sitionix.forgeit.domain.contract.clean.DbCleanupPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DefaultDbCleanupPlan implements DbCleanupPlan {

    private final Map<CleanupPhase, List<DbContract<?>>> mapping;

    private DefaultDbCleanupPlan(final Map<CleanupPhase, List<DbContract<?>>> mapping) {
        this.mapping = mapping;
    }

    @Override
    public List<DbContract<?>> contractsFor(final CleanupPhase phase) {
        final List<DbContract<?>> contracts = this.mapping.get(phase);
        if (contracts == null || contracts.isEmpty()) {
            return List.of();
        }
        return contracts;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<CleanupPhase, List<DbContract<?>>> mapping =
                new EnumMap<>(CleanupPhase.class);

        public Builder add(final CleanupPhase phase, final DbContract<?>... contracts) {
            if (phase == null || contracts == null || contracts.length == 0) {
                return this;
            }

            final List<DbContract<?>> existing =
                    this.mapping.getOrDefault(phase, new ArrayList<>());

            final List<DbContract<?>> merged = new ArrayList<>(existing);
            Collections.addAll(merged, contracts);

            this.mapping.put(phase, merged);
            return this;
        }

        public DbCleanupPlan build() {
            final Map<CleanupPhase, List<DbContract<?>>> immutable =
                    new EnumMap<>(CleanupPhase.class);

            for (final Map.Entry<CleanupPhase, List<DbContract<?>>> entry : this.mapping.entrySet()) {
                immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            return new DefaultDbCleanupPlan(Collections.unmodifiableMap(immutable));
        }
    }
}
