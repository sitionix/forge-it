package com.sitionix.forgeit.application.sql;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractInvocation;
import com.sitionix.forgeit.domain.contract.DbContractsDsl;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import com.sitionix.forgeit.domain.contract.body.JsonBodySource;
import com.sitionix.forgeit.domain.contract.graph.DefaultDbGraphContext;
import com.sitionix.forgeit.domain.ForgeItConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DbContractDefaultsTests {

    @Test
    void shouldFailWhenDefaultBodyMissing() {
        final DbContract<SampleEntity> contract = DbContractsDsl.entity(SampleEntity.class).build();

        assertThrows(ForgeItConfigurationException.class, () -> contract.withJson(null));
    }

    @Test
    void shouldUseCustomBodyWhenDefaultMissing() {
        final DbContract<SampleEntity> contract = DbContractsDsl.entity(SampleEntity.class).build();

        final DbContractInvocation<SampleEntity> invocation = contract.withJson("custom_entity.json");

        assertEquals(JsonBodySource.JSON, invocation.getBodySpecification().getSource());
        assertEquals("custom_entity.json", invocation.getBodySpecification().getResourceName());
    }

    @Test
    void shouldSkipDependenciesWhenEntityMissing() {
        final AtomicBoolean attached = new AtomicBoolean(false);
        final DbContract<ParentEntity> parent = DbContractsDsl.entity(ParentEntity.class).build();
        final DbContract<ChildEntity> child = DbContractsDsl.entity(ChildEntity.class)
                .dependsOn(parent, (childEntity, parentEntity) -> attached.set(true))
                .build();

        final DbEntityFactory factory = new DbEntityFactory() {
            @Override
            public <E> E create(final DbContractInvocation<E> invocation) {
                return null;
            }
        };
        final DefaultDbGraphContext context = new DefaultDbGraphContext(factory);
        final DbContractInvocation<ChildEntity> invocation = child.withEntity(null);

        assertDoesNotThrow(() -> context.getOrCreate(invocation));
        assertFalse(attached.get());
    }

    static final class SampleEntity {
    }

    static final class ParentEntity {
    }

    static final class ChildEntity {
    }
}
