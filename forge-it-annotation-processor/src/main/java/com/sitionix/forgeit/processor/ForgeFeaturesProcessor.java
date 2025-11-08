package com.sitionix.forgeit.processor;

import com.google.auto.service.AutoService;
import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public final class ForgeFeaturesProcessor extends AbstractProcessor {
    private static final String FORGE_IT_FQN = "com.sitionix.forgeit.core.api.ForgeIT";
    private static final ClassName GENERATED_FEATURES =
            ClassName.get("com.sitionix.forgeit.core.generated", "ForgeITFeatures");

    private static final String FEATURE_SUPPORT_FQN = "com.sitionix.forgeit.core.marker.FeatureSupport";

    private Messager messager;
    private Elements elements;
    private Types types;
    private final Set<String> aggregatedSupports = new LinkedHashSet<>();
    private final Set<String> processedContracts = new LinkedHashSet<>();

    private final Set<String> allowedFeatures = new LinkedHashSet<>();
    private final Set<Path> scannedFeatureDirectories = new LinkedHashSet<>();
    private final Set<Path> scannedFeatureJars = new LinkedHashSet<>();

    private boolean featuresGenerated;
    private String lastEmittedSignature = "";
    private boolean classpathFeaturesLoaded;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        loadClasspathFeatures();

        for (Element element : roundEnv.getElementsAnnotatedWith(ForgeFeatures.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                this.messager.printMessage(Kind.ERROR, "@ForgeFeatures can only be applied to interfaces", element);
                continue;
            }

            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                this.messager.printMessage(Kind.ERROR, "@ForgeFeatures cannot be applied to private interfaces", element);
                continue;
            }

            TypeElement interfaceElement = (TypeElement) element;
            if (!extendsForgeIT(interfaceElement)) {
                this.messager.printMessage(Kind.ERROR, "@ForgeFeatures interfaces must extend ForgeIT", element);
                continue;
            }

            ForgeFeatures forgeFeatures = interfaceElement.getAnnotation(ForgeFeatures.class);
            Collection<? extends TypeMirror> featureTypes = extractFeatureTypes(forgeFeatures);
            if (featureTypes.isEmpty()) {
                this.messager.printMessage(Kind.ERROR, "@ForgeFeatures must declare at least one feature", element);
                continue;
            }

            for (TypeMirror featureMirror : featureTypes) {
                TypeElement featureElement = asTypeElement(featureMirror);
                if (featureElement == null) {
                    this.messager.printMessage(Kind.ERROR, "Unable to resolve feature type", element);
                    continue;
                }

                if (!isFeatureSupport(featureElement)) {
                    messager.printMessage(Kind.ERROR,
                            "Each entry in @ForgeFeatures must extend FeatureSupport: " + featureElement.getQualifiedName(), element);
                    continue;
                }
                if (!isWhitelisted(featureElement)) {
                    messager.printMessage(Kind.ERROR,
                            "Feature is not registered. Add it to META-INF/forge-it/features: " + featureElement.getQualifiedName(), element);
                    continue;
                }

                this.collectFeatureContracts(featureElement, element);
            }
        }

        this.reconcileGeneratedInterface();
        return false;
    }

    private boolean isFeatureSupport(TypeElement type) {
        var featureSupport = elements.getTypeElement(FEATURE_SUPPORT_FQN);
        return featureSupport != null && types.isAssignable(type.asType(), featureSupport.asType());
    }

    private boolean isWhitelisted(TypeElement type) {
        String qualifiedName = type.getQualifiedName().toString();
        if (allowedFeatures.contains(qualifiedName)) {
            return true;
        }

        loadFeatureContainer(type);
        return allowedFeatures.contains(qualifiedName);
    }


    private void collectFeatureContracts(TypeElement featureElement, Element sourceElement) {
        if (featureElement.getKind() != ElementKind.INTERFACE) {
            this.messager.printMessage(Kind.ERROR,
                    "@ForgeFeatures values must be interfaces", sourceElement);
            return;
        }

        final String featureName = featureElement.getQualifiedName().toString();
        if (!this.processedContracts.add(featureName)) {
            return;
        }

        this.aggregatedSupports.add(featureName);

        for (TypeMirror parentInterface : featureElement.getInterfaces()) {
            TypeElement parentElement = asTypeElement(parentInterface);
            if (parentElement != null) {
                this.collectFeatureContracts(parentElement, sourceElement);
            } else {
                this.aggregatedSupports.add(parentInterface.toString());
            }
        }
    }

    private void reconcileGeneratedInterface() {
        final String signature = String.join("\n", aggregatedSupports);
        if (!this.featuresGenerated) {
            this.emitGeneratedInterface(signature);
            return;
        }

        if (!this.lastEmittedSignature.equals(signature)) {
            try {
                this.processingEnv.getFiler()
                        .getResource(StandardLocation.SOURCE_OUTPUT,
                                GENERATED_FEATURES.packageName(),
                                GENERATED_FEATURES.simpleName() + ".java")
                        .delete();
            } catch (IOException ignored) {
                // best-effort delete; if it fails we'll try overwriting below
            }
            this.emitGeneratedInterface(signature);
        }
    }

    private Collection<? extends TypeMirror> extractFeatureTypes(ForgeFeatures annotation) {
        try {
            annotation.value();
        } catch (MirroredTypesException ex) {
            return ex.getTypeMirrors();
        }
        return java.util.List.of();
    }

    private boolean extendsForgeIT(TypeElement candidate) {
        if (candidate.getQualifiedName().contentEquals(FORGE_IT_FQN)) {
            return true;
        }

        TypeElement forgeIt = this.elements.getTypeElement(FORGE_IT_FQN);
        if (forgeIt == null) {
            this.messager.printMessage(Kind.ERROR, "ForgeIT type was not found on the compilation classpath");
            return false;
        }
        return implementsInterface(candidate, forgeIt.asType());
    }

    private boolean implementsInterface(TypeElement candidate, TypeMirror targetInterface) {
        for (TypeMirror iface : candidate.getInterfaces()) {
            if (this.types.isSameType(iface, targetInterface)) {
                return true;
            }
            Element element = this.types.asElement(iface);
            if (element instanceof TypeElement typeElement && implementsInterface(typeElement, targetInterface)) {
                return true;
            }
        }

        TypeMirror superclass = candidate.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE) {
            Element element = this.types.asElement(superclass);
            return element instanceof TypeElement typeElement && implementsInterface(typeElement, targetInterface);
        }
        return false;
    }

    private TypeElement asTypeElement(TypeMirror mirror) {
        if (mirror instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                return typeElement;
            }
        }
        return null;
    }

    private void loadClasspathFeatures() {
        if (classpathFeaturesLoaded) {
            return;
        }
        classpathFeaturesLoaded = true;

        // The annotation processor runs in a dedicated class loader that is typically isolated
        // from the compilation class path. For feature discovery we need to consult both the
        // processor path and the compile class path resources.
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
            messager.printMessage(Kind.WARNING, "Failed to load ForgeIT feature declarations: " + ex.getMessage());
        }
    }

    private void loadFeaturesFromLocation(StandardLocation location) {
        loadFeaturesWithFiler(location);
    }

    private void loadFeaturesWithFiler(StandardLocation location) {
        try {
            var filer = this.processingEnv.getFiler();
            FileObject resource = filer.getResource(location, "", "META-INF/forge-it/features");
            try (InputStream stream = resource.openInputStream()) {
                readFeatureDeclarations(stream);
            }
        } catch (FilerException | FileNotFoundException ex) {
            // No declaration available at this location – ignore quietly.
        } catch (IOException ex) {
            messager.printMessage(Kind.WARNING, "Failed to load ForgeIT feature declarations: " + ex.getMessage());
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
                messager.printMessage(Kind.WARNING,
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
            classResource = processingEnv.getFiler()
                    .getResource(StandardLocation.CLASS_PATH, "", resourceName);
        } catch (FileNotFoundException ignored) {
            return;
        } catch (IOException ex) {
            messager.printMessage(Kind.WARNING,
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
            messager.printMessage(Kind.WARNING,
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
            messager.printMessage(Kind.WARNING,
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
            messager.printMessage(Kind.WARNING,
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


    private void emitGeneratedInterface(String signature) {
        TypeSpec.Builder typeBuilder = TypeSpec.interfaceBuilder(GENERATED_FEATURES.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Generated by {@link $L}.\n", ForgeFeaturesProcessor.class.getName());

        for (String support : aggregatedSupports) {
            typeBuilder.addSuperinterface(ClassName.bestGuess(support));
        }

        JavaFile javaFile = JavaFile.builder(GENERATED_FEATURES.packageName(), typeBuilder.build())
                .skipJavaLangImports(true)
                .build();

        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(
                    GENERATED_FEATURES.packageName() + "." + GENERATED_FEATURES.simpleName());
            try (Writer writer = sourceFile.openWriter()) {
                javaFile.writeTo(writer);
            }
            featuresGenerated = true;
            lastEmittedSignature = signature;
        } catch (FilerException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "ForgeIT features interface already exists and could not be replaced: " + ex.getMessage());
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate ForgeIT features interface: " + ex.getMessage());
        }
    }
}
