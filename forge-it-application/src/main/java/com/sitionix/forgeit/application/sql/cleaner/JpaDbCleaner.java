package com.sitionix.forgeit.application.sql.cleaner;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.clean.DbCleaner;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(RelationalFeatureMarker.class)
public class JpaDbCleaner implements DbCleaner {


    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void clearTables(final List<DbContract<?>> contracts) {
        final TransactionTemplate tx = new TransactionTemplate(this.transactionManager);

        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        tx.execute(status -> {

            final List<Class<?>> entityTypes = contracts.stream()
                    .filter(this::isDeletable)
                    .map(DbContract::entityType)
                    .distinct()
                    .collect(Collectors.toList());

            for (final Class<?> entityClass : entityTypes) {
                this.entityManager
                        .createQuery("DELETE FROM " + entityClass.getSimpleName() + " e")
                        .executeUpdate();
            }
            this.entityManager.flush();
            return null;
        });
    }

    private <E> boolean isDeletable(final DbContract<E> eDbContract) {
        return eDbContract.cleanupPolicy().isDeletable();
    }
}
