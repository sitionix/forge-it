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

    private boolean classpathFeaturesLoaded;

    FeatureRegistry(ProcessingEnvironment processingEnv, Messager messager, Elements elements) {
        this.processingEnv = processingEnv;
        this.messager = messager;
        this.elements = elements;
    }

    boolean isWhitelisted(TypeElement typeElement) {
        ensureClasspathFeaturesLoaded();
        String qualifiedName = typeElement.getQualifiedName().toString();
        if (allowedFeatures.contains(qualifiedName)) {
            return true;
        }
        loadFeatureContainer(typeElement);
        return allowedFeatures.contains(qualifiedName);
    }

    private void ensureClasspathFeaturesLoaded() {
        if (classpathFeaturesLoaded) {
            return;
        }
        classpathFeaturesLoaded = true;

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
            var resources = classLoader.getResources("META-INF/forge-it/features");
            while (resources.hasMoreElements()) {
                try (InputStream stream = resources.nextElement().openStream()) {
                    if (stream == null) {
                        continue;
                    }
                    readFeatureDeclarations(stream);
                }
            }
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "Failed to load ForgeIT feature declarations: " + ex.getMessage());
        }
    }

    private void loadFeaturesFromLocation(StandardLocation location) {
        try {
            FileObject resource = processingEnv.getFiler().getResource(location, "", "META-INF/forge-it/features");
            try (InputStream stream = resource.openInputStream()) {
                readFeatureDeclarations(stream);
            }
        } catch (FilerException | FileNotFoundException ex) {
            // No declaration available at this location – ignore quietly.
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "Failed to load ForgeIT feature declarations: " + ex.getMessage());
        }
    }

    private void loadFeaturesFromSystemClasspath() {
        String classPath = System.getProperty("java.class.path");
        if (classPath == null || classPath.isBlank()) {
            return;
        }

        for (String entry : classPath.split(File.pathSeparator)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            Path path;
            try {
                path = Path.of(entry);
            } catch (Exception ex) {
                messager.printMessage(Diagnostic.Kind.WARNING,
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

            String fileName = path.getFileName().toString();
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
                    .forEach(allowedFeatures::add);
        }
    }

    private void loadFeatureContainer(TypeElement featureElement) {
        String binaryName = elements.getBinaryName(featureElement).toString();
        String resourceName = binaryName.replace('.', '/') + ".class";

        FileObject classResource;
        try {
            classResource = processingEnv.getFiler().getResource(StandardLocation.CLASS_PATH, "", resourceName);
        } catch (FileNotFoundException ignored) {
            return;
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "Unable to resolve compiled class for " + binaryName + ": " + ex.getMessage());
            return;
        }

        URI classUri = classResource.toUri();
        if (classUri == null) {
            return;
        }

        String scheme = classUri.getScheme();
        try {
            if (scheme != null && scheme.equalsIgnoreCase("jar")) {
                URL url = classUri.toURL();
                URLConnection connection = url.openConnection();
                if (connection instanceof JarURLConnection jarConnection) {
                    URL jarFileUrl = jarConnection.getJarFileURL();
                    loadFeaturesFromJar(Path.of(jarFileUrl.toURI()));
                }
                return;
            }

            if (scheme != null && scheme.equalsIgnoreCase("file")) {
                Path classFile = Path.of(classUri);
                Path root = locateClassOutputRoot(classFile, resourceName);
                if (root != null) {
                    loadFeaturesFromDirectory(root);
                }
                return;
            }

            String uriString = classUri.toString();
            if (uriString.startsWith("jar:")) {
                URL url = new URL(uriString);
                URLConnection connection = url.openConnection();
                if (connection instanceof JarURLConnection jarConnection) {
                    URL jarFileUrl = jarConnection.getJarFileURL();
                    loadFeaturesFromJar(Path.of(jarFileUrl.toURI()));
                }
                return;
            }

            if (scheme == null) {
                Path classFile = Path.of(classUri);
                Path root = locateClassOutputRoot(classFile, resourceName);
                if (root != null) {
                    loadFeaturesFromDirectory(root);
                }
            }
        } catch (Exception ex) {
            messager.printMessage(Diagnostic.Kind.WARNING,
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

        if (!scannedFeatureDirectories.add(root)) {
            return;
        }

        Path featureFile = root.resolve("META-INF/forge-it/features");
        if (!Files.isRegularFile(featureFile)) {
            return;
        }

        try (InputStream stream = Files.newInputStream(featureFile)) {
            readFeatureDeclarations(stream);
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "Failed to read ForgeIT feature declarations from " + featureFile + ": " + ex.getMessage());
        }
    }

    private void loadFeaturesFromJar(Path jarPath) {
        Path normalized = jarPath.toAbsolutePath().normalize();
        if (!scannedFeatureJars.add(normalized)) {
            return;
        }

        try (JarFile jarFile = new JarFile(normalized.toFile())) {
            JarEntry featureEntry = jarFile.getJarEntry("META-INF/forge-it/features");
            if (featureEntry == null) {
                return;
            }
            try (InputStream stream = jarFile.getInputStream(featureEntry)) {
                readFeatureDeclarations(stream);
            }
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "Failed to load ForgeIT feature declarations from " + normalized + ": " + ex.getMessage());
        }
    }

    private Path locateClassOutputRoot(Path classFile, String resourceName) {
        String[] segments = resourceName.split("/");
        Path root = classFile;
        for (String ignored : segments) {
            if (root == null) {
                return null;
            }
            root = root.getParent();
        }
        return root;
    }
}
