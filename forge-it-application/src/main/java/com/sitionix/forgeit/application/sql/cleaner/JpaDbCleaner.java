package com.sitionix.forgeit.application.sql.cleaner;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.clean.DbCleaner;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(RelationalFeatureMarker.class)
public class JpaDbCleaner implements DbCleaner {


    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

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

            final boolean useTruncate = this.shouldTruncate();
            final List<String> tableNames = useTruncate ? this.resolveTableNames(entityTypes) : List.of();
            if (useTruncate && !tableNames.isEmpty()) {
                final String statement = "TRUNCATE TABLE " + String.join(", ", tableNames)
                        + " RESTART IDENTITY CASCADE";
                this.entityManager.createNativeQuery(statement).executeUpdate();
            }

            for (final Class<?> entityClass : this.resolveDeleteEntities(entityTypes, tableNames)) {
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

    private boolean shouldTruncate() {
        final DialectType dialect = this.resolveDialectType();
        return dialect == DialectType.POSTGRES;
    }

    private DialectType resolveDialectType() {
        final String dialectClassName = this.resolveDialectClassName();
        if (!StringUtils.hasText(dialectClassName)) {
            return DialectType.UNKNOWN;
        }
        final String normalized = dialectClassName.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case String value when value.contains("postgres") -> DialectType.POSTGRES;
            case String value when value.contains("mariadb") -> DialectType.MARIADB;
            case String value when value.contains("mysql") -> DialectType.MYSQL;
            case String value when value.contains("sqlserver") || value.contains("mssql") -> DialectType.MSSQL;
            case String value when value.contains("oracle") -> DialectType.ORACLE;
            case String value when value.contains("h2") -> DialectType.H2;
            default -> DialectType.UNKNOWN;
        };
    }

    private String resolveDialectClassName() {
        final Object configuredDialect = this.entityManagerFactory.getProperties().get("hibernate.dialect");
        if (configuredDialect instanceof String dialectName && StringUtils.hasText(dialectName)) {
            return dialectName;
        }
        if (configuredDialect != null) {
            return configuredDialect.getClass().getName();
        }
        try {
            final Class<?> sessionFactoryClass = Class.forName("org.hibernate.SessionFactory");
            final Object sessionFactory = this.entityManagerFactory.unwrap(sessionFactoryClass);
            if (sessionFactory == null) {
                return null;
            }
            final Class<?> implementorClass = Class.forName("org.hibernate.engine.spi.SessionFactoryImplementor");
            if (!implementorClass.isInstance(sessionFactory)) {
                return null;
            }
            final Object jdbcServices = implementorClass.getMethod("getJdbcServices").invoke(sessionFactory);
            if (jdbcServices == null) {
                return null;
            }
            final Object dialect = jdbcServices.getClass().getMethod("getDialect").invoke(jdbcServices);
            return dialect != null ? dialect.getClass().getName() : null;
        } catch (final Exception ignored) {
            return null;
        }
    }

    private List<String> resolveTableNames(final List<Class<?>> entityTypes) {
        final Set<String> tableNames = new HashSet<>();
        for (final Class<?> entityClass : entityTypes) {
            final String tableName = this.resolveTableName(entityClass);
            if (!StringUtils.hasText(tableName)) {
                continue;
            }
            tableNames.add(tableName);
        }
        return tableNames.stream().sorted().toList();
    }

    private List<Class<?>> resolveDeleteEntities(final List<Class<?>> entityTypes, final List<String> tableNames) {
        if (tableNames.isEmpty()) {
            return entityTypes;
        }
        return entityTypes.stream()
                .filter(entityType -> !tableNames.contains(this.resolveTableName(entityType)))
                .toList();
    }

    private String resolveTableName(final Class<?> entityClass) {
        final Table table = entityClass.getAnnotation(Table.class);
        if (table == null || !StringUtils.hasText(table.name())) {
            return null;
        }
        final String name = table.name().trim();
        if (StringUtils.hasText(table.schema())) {
            return table.schema().trim() + "." + name;
        }
        return name;
    }

    private enum DialectType {
        POSTGRES,
        MYSQL,
        MARIADB,
        MSSQL,
        ORACLE,
        H2,
        UNKNOWN
    }
}
