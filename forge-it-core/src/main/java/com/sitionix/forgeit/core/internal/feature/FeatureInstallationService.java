package com.sitionix.forgeit.core.internal.feature;

import com.sitionix.forgeit.core.marker.FeatureSupport;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Discovers feature installers and validates feature declarations against the
 * classpath whitelist.
 */
@Log4j2
public final class FeatureInstallationService {

    private static final String FEATURE_RESOURCE = "META-INF/forge-it/features";

    private final Map<Class<? extends FeatureSupport>, FeatureInstaller> installers;
    private final Set<String> whitelistedFeatures;

    /**
     * Default constructor that uses this class' ClassLoader.
     */
    public FeatureInstallationService() {
        this(FeatureInstallationService.class.getClassLoader());
    }

    /**
     * Constructor with explicit ClassLoader (kept for flexibility/backward compatibility).
     */
    public FeatureInstallationService(ClassLoader classLoader) {
        log.info("Initializing FeatureInstallationService with classloader: {}", classLoader);
        this.installers = indexInstallers(classLoader);
        this.whitelistedFeatures = Collections.unmodifiableSet(readWhitelist(classLoader));
        log.info("FeatureInstallationService initialized. Installers: {}, whitelisted features: {}",
                this.installers.keySet(), this.whitelistedFeatures);
    }

    public void installFeatures(Collection<Class<? extends FeatureSupport>> features,
                                FeatureInstallationContext context) {
        if (features == null || features.isEmpty()) {
            log.debug("No features requested for installation");
            return;
        }

        log.info("Installing features: {}", features);

        for (Class<? extends FeatureSupport> feature : features) {
            log.debug("Validating feature: {}", feature);
            validateFeature(feature);

            final FeatureInstaller installer = this.installers.get(feature);
            if (installer == null) {
                log.error("No FeatureInstaller registered for feature: {}", feature.getName());
                throw new IllegalStateException("No FeatureInstaller registered for " + feature.getName());
            }

            log.info("Installing feature: {} using installer: {}", feature.getName(), installer.getClass().getName());
            installer.install(context);
        }
    }

    private void validateFeature(Class<? extends FeatureSupport> feature) {
        if (!FeatureSupport.class.isAssignableFrom(feature)) {
            log.error("Class {} does not implement FeatureSupport", feature.getName());
            throw new IllegalArgumentException(feature.getName() + " does not implement FeatureSupport");
        }
        if (!this.whitelistedFeatures.contains(feature.getName())) {
            log.error("Feature {} is not whitelisted. Whitelist: {}", feature.getName(), this.whitelistedFeatures);
            throw new IllegalStateException(
                    "Feature " + feature.getName() + " is not whitelisted on the classpath");
        }
        log.debug("Feature {} passed validation", feature.getName());
    }

    private static Map<Class<? extends FeatureSupport>, FeatureInstaller> indexInstallers(ClassLoader classLoader) {
        log.debug("Indexing FeatureInstallers using classloader: {}", classLoader);

        final List<FeatureInstaller> installers =
                SpringFactoriesLoader.loadFactories(FeatureInstaller.class, classLoader);

        log.info("Discovered {} FeatureInstaller implementations: {}",
                installers.size(),
                installers.stream().map(i -> i.getClass().getName()).toList());

        final Map<Class<? extends FeatureSupport>, FeatureInstaller> indexed = new LinkedHashMap<>();
        for (FeatureInstaller installer : installers) {
            final Class<? extends FeatureSupport> featureType = installer.featureType();
            final FeatureInstaller previous = indexed.putIfAbsent(featureType, installer);
            if (previous != null) {
                log.error("Multiple FeatureInstaller implementations registered for feature type {}: {}, {}",
                        featureType.getName(), previous.getClass().getName(), installer.getClass().getName());
                throw new IllegalStateException(
                        "Multiple FeatureInstaller implementations registered for " + featureType.getName());
            }
            log.debug("Registered installer {} for feature {}", installer.getClass().getName(), featureType.getName());
        }
        return Collections.unmodifiableMap(indexed);
    }

    private static Set<String> readWhitelist(ClassLoader classLoader) {
        log.debug("Reading ForgeIT feature whitelist from resources '{}' using classloader: {}",
                FEATURE_RESOURCE, classLoader);

        try {
            final Enumeration<URL> resources = classLoader.getResources(FEATURE_RESOURCE);
            final Set<String> features = new LinkedHashSet<>();

            if (!resources.hasMoreElements()) {
                log.warn("No '{}' resources found on classpath for classloader: {}",
                        FEATURE_RESOURCE, classLoader);
            }

            while (resources.hasMoreElements()) {
                final URL resource = resources.nextElement();
                log.info("Found feature whitelist resource: {}", resource);

                try (InputStream inputStream = resource.openStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                    reader.lines()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty())
                            .filter(line -> !line.startsWith("#"))
                            .peek(line -> log.debug("Whitelist entry discovered: {}", line))
                            .forEach(features::add);
                }
            }

            log.info("Final ForgeIT feature whitelist ({} entries): {}", features.size(), features);
            return features;
        } catch (IOException ex) {
            log.error("Failed to read ForgeIT feature whitelist from '{}'", FEATURE_RESOURCE, ex);
            throw new IllegalStateException("Failed to read ForgeIT feature whitelist", ex);
        }
    }
}
