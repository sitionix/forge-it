package com.sitionix.forgeit.application.sql.cleaner;

import com.sitionix.forgeit.core.contract.ForgeDbContracts;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.config.BeanDefinition;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public final class ReflectiveDbContractsRegistry implements DbContractsRegistry {

    private static final Logger log =
            LoggerFactory.getLogger(ReflectiveDbContractsRegistry.class);

    private final List<DbContract<?>> cachedContracts;

    public ReflectiveDbContractsRegistry() {
        this.cachedContracts = this.discoverContracts();
    }

    @Override
    public List<DbContract<?>> allContracts() {
        return this.cachedContracts;
    }

    private List<DbContract<?>> discoverContracts() {
        final List<DbContract<?>> result = new ArrayList<>();

        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ForgeDbContracts.class));

        for (final BeanDefinition candidate : scanner.findCandidateComponents("")) {
            final String className = candidate.getBeanClassName();
            try {
                final Class<?> holderClass = Class.forName(className);

                log.debug("Inspecting @ForgeDbContracts class {}", holderClass.getName());

                for (final Field field : holderClass.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    if (!DbContract.class.isAssignableFrom(field.getType())) {
                        continue;
                    }

                    field.setAccessible(true);
                    final Object value = field.get(null);
                    if (value instanceof final DbContract<?> contract) {
                        result.add(contract);
                        log.debug("Discovered DbContract {}.{}",
                                holderClass.getSimpleName(), field.getName());
                    }
                }
            } catch (final ClassNotFoundException e) {
                log.error("Failed to load @ForgeDbContracts class {}", className, e);
            } catch (final IllegalAccessException e) {
                log.error("Failed to read DbContract static fields from {}", className, e);
            }
        }

        return Collections.unmodifiableList(result);
    }
}
