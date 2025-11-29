package com.sitionix.forgeit.processor;

import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class FeatureRegistry {

    private final ProcessingEnvironment processingEnv;
    private final Messager messager;
    private final Elements elements;

    private final Set<String> allowedFeatures = new LinkedHashSet<>();
    private final Set<Path> scannedFeatureDirectories = new LinkedHashSet<>();
    private final Set<Path> scannedFeatureJars = new LinkedHashSet<>();
    private final Set<String> implicitlyRegisteredFeatures = new LinkedHashSet<>();

    private boolean classpathFeaturesLoaded;

    FeatureRegistry(ProcessingEnvironment processingEnv, Messager messager, Elements elements) {
        this.processingEnv = processingEnv;
        this.messager = messager;
        this.elements = elements;
    }

    boolean isWhitelisted(TypeElement typeElement) {
        ensureClasspathFeaturesLoaded();
        final String qualifiedName = typeElement.getQualifiedName().toString();
        if (this.allowedFeatures.contains(qualifiedName)) {
            return true;
        }
        loadFeatureContainer(typeElement);
        if (this.allowedFeatures.contains(qualifiedName)) {
            return true;
        }

        if (this.implicitlyRegisteredFeatures.add(qualifiedName)) {
            this.allowedFeatures.add(qualifiedName);
            this.messager.printMessage(Diagnostic.Kind.WARNING,
                    "ForgeIT feature '" + qualifiedName
                            + "' has no META-INF/forge-it/features declaration; registering implicitly.");
        }
        return true;
    }

    private void ensureClasspathFeaturesLoaded() {
        if (this.classpathFeaturesLoaded) {
            return;
        }
        this.classpathFeaturesLoaded = true;

        loadFeaturesFromClassLoader(Thread.currentThread().getContextClassLoader());
        loadFeaturesFromClassLoader(ForgeFeaturesProcessor.class.getClassLoader());
        loadFeaturesFromLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH);
        loadFeaturesFromLocation(StandardLocation.CLASS_PATH);
        loadFeaturesFromSystemClasspath();
    }

    private void loadFeaturesFromClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            final var resources = classLoader.getResources("META-INF/forge-it/features");
            while (resources.hasMoreElements()) {
                try (InputStream stream = resources.nextElement().openStream()) {
                    if (stream == null) {
                        continue;
                    }
                    readFeatureDeclarations(stream);
                }
            }
        } catch (IOException ex) {
            this.messager.printMessage(Diagnostic.Kind.WARNING,
                    "Failed to load ForgeIT feature declarations: " + ex.getMessage());
        }
    }

    private void loadFeaturesFromLocation(StandardLocation location) {
        try {
            final FileObject resource = this.processingEnv.getFiler().getResource(location, "", "META-INF/forge-it/features");
            try (InputStream stream = resource.openInputStream()) {
                readFeatureDeclarations(stream);
            }
        } catch (FilerException | FileNotFoundException ex) {
            // No declaration available at this location – ignore quietly.
        } catch (IOException ex) {
            this.messager.printMessage(Diagnostic.Kind.WARNING,
                    "Failed to load ForgeIT feature declarations: " + ex.getMessage());
        }
    }

    private void loadFeaturesFromSystemClasspath() {
        final String classPath = System.getProperty("java.class.path");
        if (classPath == null || classPath.isBlank()) {
            return;
        }

        for (final String entry : classPath.split(File.pathSeparator)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            final Path path;
            try {
                path = Path.of(entry);
            } catch (Exception ex) {
                this.messager.printMessage(Diagnostic.Kind.WARNING,
                        "Failed to interpret classpath entry '" + entry + "': " + ex.getMessage());
                continue;
            }

            if (Files.isDirectory(path)) {
                loadFeaturesFromDirectory(path);
                continue;
            }

            if (!Files.isRegularFile(path)) {
                continue;
            }

            final String fileName = path.getFileName().toString();
            if (!fileName.endsWith(".jar") && !fileName.endsWith(".JAR")) {
                continue;
            }

            loadFeaturesFromJar(path);
        }
    }

    private void readFeatureDeclarations(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(this.allowedFeatures::add);
        }
    }

    private void loadFeatureContainer(TypeElement featureElement) {
        final String binaryName = this.elements.getBinaryName(featureElement).toString();
        final String resourceName = binaryName.replace('.', '/') + ".class";

        final FileObject classResource;
        try {
            classResource = this.processingEnv.getFiler().getResource(StandardLocation.CLASS_PATH, "", resourceName);
        } catch (FileNotFoundException ignored) {
            return;
        } catch (IOException ex) {
            this.messager.printMessage(Diagnostic.Kind.WARNING,
                    "Unable to resolve compiled class for " + binaryName + ": " + ex.getMessage());
            return;
        }

        final URI classUri = classResource.toUri();
        if (classUri == null) {
            return;
        }

        final String scheme = classUri.getScheme();
        try {
            if (scheme != null && scheme.equalsIgnoreCase("jar")) {
                final URL url = classUri.toURL();
                final URLConnection connection = url.openConnection();
                if (connection instanceof JarURLConnection jarConnection) {
                    final URL jarFileUrl = jarConnection.getJarFileURL();
                    loadFeaturesFromJar(Path.of(jarFileUrl.toURI()));
                }
                return;
            }

            if (scheme != null && scheme.equalsIgnoreCase("file")) {
                final Path classFile = Path.of(classUri);
                final Path root = locateClassOutputRoot(classFile, resourceName);
                if (root != null) {
                    loadFeaturesFromDirectory(root);
                }
                return;
            }

            final String uriString = classUri.toString();
            if (uriString.startsWith("jar:")) {
                final URL url = new URL(uriString);
                final URLConnection connection = url.openConnection();
                if (connection instanceof JarURLConnection jarConnection) {
                    final URL jarFileUrl = jarConnection.getJarFileURL();
                    loadFeaturesFromJar(Path.of(jarFileUrl.toURI()));
                }
                return;
            }

            if (scheme == null) {
                final Path classFile = Path.of(classUri);
                final Path root = locateClassOutputRoot(classFile, resourceName);
                if (root != null) {
                    loadFeaturesFromDirectory(root);
                }
            }
        } catch (Exception ex) {
            this.messager.printMessage(Diagnostic.Kind.WARNING,
                    "Failed to resolve feature declarations for " + binaryName + ": " + ex.getMessage());
        }
    }

    private void loadFeaturesFromDirectory(Path directory) {
        Path root;
        try {
            root = directory.toRealPath();
        } catch (IOException ex) {
            root = directory.toAbsolutePath().normalize();
        }

        if (!this.scannedFeatureDirectories.add(root)) {
            return;
        }

        final Path featureFile = root.resolve("META-INF/forge-it/features");
        if (!Files.isRegularFile(featureFile)) {
            return;
        }

        try (InputStream stream = Files.newInputStream(featureFile)) {
            readFeatureDeclarations(stream);
        } catch (IOException ex) {
            this.messager.printMessage(Diagnostic.Kind.WARNING,
                    "Failed to read ForgeIT feature declarations from " + featureFile + ": " + ex.getMessage());
        }
    }

    private void loadFeaturesFromJar(Path jarPath) {
        final Path normalized = jarPath.toAbsolutePath().normalize();
        if (!this.scannedFeatureJars.add(normalized)) {
            return;
        }

        try (JarFile jarFile = new JarFile(normalized.toFile())) {
            final JarEntry featureEntry = jarFile.getJarEntry("META-INF/forge-it/features");
            if (featureEntry == null) {
                return;
            }
            try (InputStream stream = jarFile.getInputStream(featureEntry)) {
                readFeatureDeclarations(stream);
            }
        } catch (IOException ex) {
            this.messager.printMessage(Diagnostic.Kind.WARNING,
                    "Failed to load ForgeIT feature declarations from " + normalized + ": " + ex.getMessage());
        }
    }

    private Path locateClassOutputRoot(Path classFile, String resourceName) {
        final String[] segments = resourceName.split("/");
        Path root = classFile;
        for (final String ignored : segments) {
            if (root == null) {
                return null;
            }
            root = root.getParent();
        }
        return root;
    }
}
