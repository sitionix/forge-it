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
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

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
        return allowedFeatures.contains(type.getQualifiedName().toString());
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

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = ForgeFeaturesProcessor.class.getClassLoader();
        try {
            var resources = cl.getResources("META-INF/forge-it/features");
            while (resources.hasMoreElements()) {
                try (InputStream stream = resources.nextElement().openStream()) {
                    if (stream == null) continue;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        reader.lines()
                                .map(String::trim)
                                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                                .forEach(allowedFeatures::add);
                    }
                }
            }
        } catch (IOException ex) {
            messager.printMessage(Kind.WARNING, "Failed to load ForgeIT feature declarations: " + ex.getMessage());
        }
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
