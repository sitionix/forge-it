package com.sitionix.forgeit.bundleconsumer;

import com.sitionix.forgeit.core.api.ForgeIT;
import com.sitionix.forgeit.core.internal.feature.FeatureInstaller;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BundleMetadataTest {
    @Test
    void everyBundledInstallerIsWhitelistedInTheActualJar() throws Exception {
        var location = ForgeIT.class.getProtectionDomain().getCodeSource().getLocation();
        assertThat(location.getPath()).endsWith(".jar");
        try (var jar = new JarFile(new java.io.File(location.toURI()))) {
            var entry = jar.getJarEntry("META-INF/forge-it/features");
            assertThat(entry).isNotNull();
            Set<String> whitelist;
            try (var input = jar.getInputStream(entry)) {
                whitelist = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                        .map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toSet());
            }
            var installers = SpringFactoriesLoader.loadFactories(FeatureInstaller.class, ForgeIT.class.getClassLoader());
            assertThat(installers).hasSize(7);
            for (var installer : installers) {
                assertThat(whitelist).as("bundle whitelist for %s", installer.featureType().getSimpleName())
                        .contains(installer.featureType().getName());
            }
            assertThat(whitelist).contains(
                    "com.sitionix.forgeit.mockmvc.api.MockMvcSupport",
                    "com.sitionix.forgeit.ros.api.RosSupport");
        }
    }
}
