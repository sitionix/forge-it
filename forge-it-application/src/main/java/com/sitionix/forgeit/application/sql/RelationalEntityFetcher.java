package com.sitionix.forgeit.application.sql;

import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractsRegistry;
import com.sitionix.forgeit.domain.contract.DbDependency;
import com.sitionix.forgeit.domain.model.sql.DbEntityFetcher;
import com.sitionix.forgeit.domain.model.sql.DbRetrieveFactory;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.ArrayDeque;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(RelationalFeatureMarker.class)
public class RelationalEntityFetcher implements DbEntityFetcher {

    private final DbRetrieveFactory retrieveFactory;
    private final PlatformTransactionManager transactionManager;
    private final DbContractsRegistry contractsRegistry;
    private final Map<Class<?>, DbContract<?>> contractsByType = new LinkedHashMap<>();
    private final Map<DbContract<?>, Set<DbContract<?>>> relationGraph = new LinkedHashMap<>();
    private boolean contractsIndexed;
    private final RelationFetchStrategy deepFetchStrategy = new DeepStructureFetchStrategy();
    private final RelationFetchStrategy contractFetchStrategy = new ContractRelationFetchStrategy();

    @Override
    public <E> E reloadById(final DbContract<E> contract, final Object id) {
        return this.retrieveFactory.forClass(contract.entityType()).getById(id);
    }

    @Override
    public <E> E reloadByIdWithRelations(final DbContract<E> contract, final Object id) {
        final TransactionTemplate template = new TransactionTemplate(this.transactionManager);
        return template.execute(status -> {
            final E entity = this.retrieveFactory.forClass(contract.entityType()).getById(id);
            this.resolveStrategy(contract).fetch(entity, contract);
            return entity;
        });
    }

    @Override
    public <E> List<E> loadAll(final Class<E> entityType) {
        return this.retrieveFactory.forClass(entityType).getAll();
    }

    @Override
    public <E> List<E> loadAllWithRelations(final Class<E> entityType) {
        final TransactionTemplate template = new TransactionTemplate(this.transactionManager);
        final List<E> loaded = template.execute(status -> {
            final List<E> entities = this.retrieveFactory.forClass(entityType).getAll();
            final DbContract<?> contract = this.resolveContract(entityType);
            final RelationFetchStrategy strategy = this.resolveStrategy(contract);
            for (final E entity : entities) {
                strategy.fetch(entity, contract);
            }
            return entities;
        });
        return loaded == null ? new ArrayList<>() : loaded;
    }

    @Override
    public <E> List<E> loadAllWithRelations(final DbContract<E> contract) {
        final TransactionTemplate template = new TransactionTemplate(this.transactionManager);
        final List<E> loaded = template.execute(status -> {
            final List<E> entities = this.retrieveFactory.forClass(contract.entityType()).getAll();
            final RelationFetchStrategy strategy = this.resolveStrategy(contract);
            for (final E entity : entities) {
                strategy.fetch(entity, contract);
            }
            return entities;
        });
        return loaded == null ? new ArrayList<>() : loaded;
    }

    private RelationFetchStrategy resolveStrategy(final DbContract<?> contract) {
        return contract == null ? this.deepFetchStrategy : this.contractFetchStrategy;
    }

    private DbContract<?> resolveContract(final Class<?> entityType) {
        this.indexContractsIfNeeded();
        return this.contractsByType.get(entityType);
    }

    private void indexContractsIfNeeded() {
        if (this.contractsIndexed) {
            return;
        }
        final List<DbContract<?>> contracts = this.contractsRegistry == null
                ? Collections.emptyList()
                : this.contractsRegistry.allContracts();
        for (final DbContract<?> contract : contracts) {
            this.registerContract(contract);
        }
        this.contractsIndexed = true;
    }

    private void registerContract(final DbContract<?> contract) {
        if (contract == null) {
            return;
        }
        this.contractsByType.putIfAbsent(contract.entityType(), contract);
        this.registerDependencies(contract);
    }

    private void registerDependencies(final DbContract<?> contract) {
        final List<? extends DbDependency<?, ?>> dependencies = contract.dependencies();
        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }
        for (final DbDependency<?, ?> dependency : dependencies) {
            if (dependency == null || dependency.parent() == null) {
                continue;
            }
            this.relationGraph
                    .computeIfAbsent(contract, ignored -> new LinkedHashSet<>())
                    .add(dependency.parent());
            this.relationGraph
                    .computeIfAbsent(dependency.parent(), ignored -> new LinkedHashSet<>())
                    .add(contract);
        }
    }

    private void initializeDeepStructure(final Object entity) {
        if (entity == null) {
            return;
        }
        final BeanWrapper wrapper = new BeanWrapperImpl(entity);
        final PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
        if (descriptors == null) {
            return;
        }
        for (final PropertyDescriptor descriptor : descriptors) {
            final String name = descriptor.getName();
            if ("class".equals(name)) {
                continue;
            }
            final Object value = wrapper.getPropertyValue(name);
            if (value == null || this.isScalar(value)) {
                continue;
            }
            this.initializeHibernateProxy(value);
            this.replaceWithUnproxy(wrapper, descriptor, value);
            if (value instanceof Iterable) {
                for (final Object ignored : (Iterable<?>) value) {
                    // touching the iterator triggers initialization
                }
            } else if (value.getClass().isArray()) {
                Array.getLength(value);
            }
        }
    }

    private void initializeHibernateProxy(final Object value) {
        try {
            final Class<?> hibernate = Class.forName("org.hibernate.Hibernate");
            final java.lang.reflect.Method initialize = hibernate.getMethod("initialize", Object.class);
            initialize.invoke(null, value);
        } catch (final ReflectiveOperationException ignored) {
            // Hibernate not on the classpath or initialization failed; rely on direct access instead.
        }
    }

    private Object unproxyIfPossible(final Object value) {
        if (value == null) {
            return null;
        }
        try {
            final Class<?> hibernate = Class.forName("org.hibernate.Hibernate");
            final java.lang.reflect.Method unproxy = hibernate.getMethod("unproxy", Object.class);
            return unproxy.invoke(null, value);
        } catch (final ReflectiveOperationException ignored) {
            return value;
        }
    }

    private Object replaceWithUnproxy(final BeanWrapper wrapper,
                                      final PropertyDescriptor descriptor,
                                      final Object value) {
        if (descriptor.getWriteMethod() == null) {
            return value;
        }
        final Object unproxied = this.unproxyIfPossible(value);
        if (unproxied == null || unproxied == value) {
            return value;
        }
        wrapper.setPropertyValue(descriptor.getName(), unproxied);
        return unproxied;
    }

    private boolean isScalar(final Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    private interface RelationFetchStrategy {
        void fetch(Object entity, DbContract<?> contract);
    }

    private final class DeepStructureFetchStrategy implements RelationFetchStrategy {
        @Override
        public void fetch(final Object entity, final DbContract<?> contract) {
            initializeDeepStructure(entity);
        }
    }

    private final class ContractRelationFetchStrategy implements RelationFetchStrategy {
        @Override
        public void fetch(final Object entity, final DbContract<?> contract) {
            if (entity == null || contract == null) {
                return;
            }
            ensureContractIndexed(contract);
            final Set<Object> visitedEntities = Collections.newSetFromMap(new IdentityHashMap<>());
            final Set<DbContract<?>> visitedContracts = new LinkedHashSet<>();
            final Queue<ContractNode> queue = new ArrayDeque<>();
            queue.add(new ContractNode(entity, contract));

            while (!queue.isEmpty()) {
                final ContractNode node = queue.poll();
                if (node.entity == null || node.contract == null) {
                    continue;
                }
                if (!visitedEntities.add(node.entity)) {
                    continue;
                }
                if (!visitedContracts.add(node.contract)) {
                    continue;
                }
                enqueueRelatedEntities(node.entity, node.contract, queue);
            }
        }

        private void ensureContractIndexed(final DbContract<?> contract) {
            indexContractsIfNeeded();
            registerContract(contract);
        }

        private void enqueueRelatedEntities(final Object entity,
                                            final DbContract<?> contract,
                                            final Queue<ContractNode> queue) {
            final Set<DbContract<?>> relatedContracts = relationGraph.get(contract);
            if (relatedContracts == null || relatedContracts.isEmpty()) {
                return;
            }
            final BeanWrapper wrapper = new BeanWrapperImpl(entity);
            final PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
            if (descriptors == null) {
                return;
            }
            for (final PropertyDescriptor descriptor : descriptors) {
                final String name = descriptor.getName();
                if ("class".equals(name) || descriptor.getReadMethod() == null) {
                    continue;
                }
                final Object value = wrapper.getPropertyValue(name);
                if (value == null || isScalar(value)) {
                    continue;
                }
                for (final DbContract<?> related : relatedContracts) {
                    if (related == null || related.entityType() == null) {
                        continue;
                    }
                    if (matchesRelation(descriptor, value, related.entityType())) {
                        initializeHibernateProxy(value);
                        final Object resolved = replaceWithUnproxy(wrapper, descriptor, value);
                        enqueueRelated(resolved, related, queue);
                    }
                }
            }
        }

        private void enqueueRelated(final Object value,
                                    final DbContract<?> related,
                                    final Queue<ContractNode> queue) {
            if (value instanceof final Iterable<?> iterable) {
                for (final Object element : iterable) {
                    queue.add(new ContractNode(element, related));
                }
                return;
            }
            if (value.getClass().isArray()) {
                final int length = Array.getLength(value);
                for (int index = 0; index < length; index++) {
                    queue.add(new ContractNode(Array.get(value, index), related));
                }
                return;
            }
            if (related.entityType().isAssignableFrom(value.getClass())) {
                queue.add(new ContractNode(value, related));
            }
        }

        private boolean matchesRelation(final PropertyDescriptor descriptor,
                                        final Object value,
                                        final Class<?> relatedType) {
            if (relatedType.isAssignableFrom(value.getClass())) {
                return true;
            }
            if (value.getClass().isArray()) {
                final Class<?> component = value.getClass().getComponentType();
                return component != null && relatedType.isAssignableFrom(component);
            }
            if (value instanceof Iterable) {
                if (isCollectionOfType(descriptor, relatedType)) {
                    return true;
                }
                for (final Object element : (Iterable<?>) value) {
                    if (element != null && relatedType.isAssignableFrom(element.getClass())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isCollectionOfType(final PropertyDescriptor descriptor, final Class<?> elementType) {
            final Type genericType = descriptor.getReadMethod().getGenericReturnType();
            if (!(genericType instanceof ParameterizedType)) {
                return false;
            }
            final ParameterizedType parameterizedType = (ParameterizedType) genericType;
            final Type[] args = parameterizedType.getActualTypeArguments();
            if (args.length != 1) {
                return false;
            }
            if (args[0] instanceof Class<?>) {
                return elementType.isAssignableFrom((Class<?>) args[0]);
            }
            if (args[0] instanceof ParameterizedType) {
                final Type raw = ((ParameterizedType) args[0]).getRawType();
                if (raw instanceof Class<?>) {
                    return elementType.isAssignableFrom((Class<?>) raw);
                }
            }
            return false;
        }
    }

    private static final class ContractNode {
        private final Object entity;
        private final DbContract<?> contract;

        private ContractNode(final Object entity, final DbContract<?> contract) {
            this.entity = entity;
            this.contract = contract;
        }
    }
}
