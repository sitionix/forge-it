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
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@SupportedAnnotationTypes("com.sitionix.forgeit.core.annotation.ForgeFeatures")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public final class ForgeFeaturesProcessor extends AbstractProcessor {
    private static final String FORGE_IT_FQN = "com.sitionix.forgeit.core.domain.ForgeIT";
    private static final ClassName GENERATED_FEATURES =
            ClassName.get("com.sitionix.forgeit.core.generated", "ForgeITFeatures");

    private Messager messager;
    private Elements elements;
    private Types types;
    private final Set<String> aggregatedSupports = new LinkedHashSet<>();
    private boolean featuresGenerated;
    private String lastEmittedSignature = "";

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

        for (Element element : roundEnv.getElementsAnnotatedWith(ForgeFeatures.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Kind.ERROR, "@ForgeFeatures can only be applied to interfaces", element);
                continue;
            }

            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                messager.printMessage(Kind.ERROR, "@ForgeFeatures cannot be applied to private interfaces", element);
                continue;
            }

            TypeElement interfaceElement = (TypeElement) element;
            if (!extendsForgeIT(interfaceElement)) {
                messager.printMessage(Kind.ERROR, "@ForgeFeatures interfaces must extend ForgeIT", element);
                continue;
            }

            ForgeFeatures forgeFeatures = interfaceElement.getAnnotation(ForgeFeatures.class);
            Collection<? extends TypeMirror> featureTypes = extractFeatureTypes(forgeFeatures);
            if (featureTypes.isEmpty()) {
                messager.printMessage(Kind.ERROR, "@ForgeFeatures must declare at least one feature", element);
                continue;
            }

            for (TypeMirror featureMirror : featureTypes) {
                TypeElement featureElement = asTypeElement(featureMirror);
                if (featureElement == null) {
                    messager.printMessage(Kind.ERROR, "Unable to resolve feature type", element);
                    continue;
                }

                this.aggregatedSupports.addAll(FeatureRegistry.resolveSupportInterfaces(featureElement.getQualifiedName().toString()));
            }
        }

        reconcileGeneratedInterface();
        return false;
    }

    private void reconcileGeneratedInterface() {
        String signature = String.join("\n", aggregatedSupports);
        if (!featuresGenerated) {
            emitGeneratedInterface(signature);
            return;
        }

        if (!lastEmittedSignature.equals(signature)) {
            try {
                processingEnv.getFiler()
                        .getResource(StandardLocation.SOURCE_OUTPUT,
                                GENERATED_FEATURES.packageName(),
                                GENERATED_FEATURES.simpleName() + ".java")
                        .delete();
            } catch (IOException ignored) {
                // best-effort delete; if it fails we'll try overwriting below
            }
            emitGeneratedInterface(signature);
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
        TypeElement forgeIt = elements.getTypeElement(FORGE_IT_FQN);
        if (forgeIt == null) {
            messager.printMessage(Kind.ERROR, "ForgeIT type was not found on the compilation classpath");
            return false;
        }
        return implementsInterface(candidate, forgeIt.asType());
    }

    private boolean implementsInterface(TypeElement candidate, TypeMirror targetInterface) {
        for (TypeMirror iface : candidate.getInterfaces()) {
            if (types.isSameType(iface, targetInterface)) {
                return true;
            }
            Element element = types.asElement(iface);
            if (element instanceof TypeElement typeElement && implementsInterface(typeElement, targetInterface)) {
                return true;
            }
        }

        TypeMirror superclass = candidate.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE) {
            Element element = types.asElement(superclass);
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
