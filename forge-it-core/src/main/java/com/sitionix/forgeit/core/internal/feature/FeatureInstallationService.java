package com.sitionix.forgeit.core.internal.feature;

import com.sitionix.forgeit.core.marker.FeatureSupport;
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
public final class FeatureInstallationService {

    private static final String FEATURE_RESOURCE = "META-INF/forge-it/features";

    private final Map<Class<? extends FeatureSupport>, FeatureInstaller> installers;
    private final Set<String> whitelistedFeatures;

    public FeatureInstallationService(ClassLoader classLoader) {
        this.installers = indexInstallers(classLoader);
        this.whitelistedFeatures = Collections.unmodifiableSet(readWhitelist(classLoader));
    }

    public void installFeatures(Collection<Class<? extends FeatureSupport>> features,
                                FeatureInstallationContext context) {
        for (Class<? extends FeatureSupport> feature : features) {
            validateFeature(feature);
            final FeatureInstaller installer = this.installers.get(feature);
            if (installer == null) {
                throw new IllegalStateException("No FeatureInstaller registered for " + feature.getName());
            }
            installer.install(context);
        }
    }

    private void validateFeature(Class<? extends FeatureSupport> feature) {
        if (!FeatureSupport.class.isAssignableFrom(feature)) {
            throw new IllegalArgumentException(feature.getName() + " does not implement FeatureSupport");
        }
        if (!this.whitelistedFeatures.contains(feature.getName())) {
            throw new IllegalStateException(
                    "Feature " + feature.getName() + " is not whitelisted on the classpath");
        }
    }

    private static Map<Class<? extends FeatureSupport>, FeatureInstaller> indexInstallers(ClassLoader classLoader) {
        final List<FeatureInstaller> installers = SpringFactoriesLoader.loadFactories(FeatureInstaller.class, classLoader);
        final Map<Class<? extends FeatureSupport>, FeatureInstaller> indexed = new LinkedHashMap<>();
        for (FeatureInstaller installer : installers) {
            final Class<? extends FeatureSupport> featureType = installer.featureType();
            final FeatureInstaller previous = indexed.putIfAbsent(featureType, installer);
            if (previous != null) {
                throw new IllegalStateException(
                        "Multiple FeatureInstaller implementations registered for " + featureType.getName());
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    private static Set<String> readWhitelist(ClassLoader classLoader) {
        try {
            final Enumeration<URL> resources = classLoader.getResources(FEATURE_RESOURCE);
            final Set<String> features = new LinkedHashSet<>();
            while (resources.hasMoreElements()) {
                final URL resource = resources.nextElement();
                try (InputStream inputStream = resource.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    reader.lines()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty())
                            .filter(line -> !line.startsWith("#"))
                            .forEach(features::add);
                }
            }
            return features;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read ForgeIT feature whitelist", ex);
        }
    }
}
