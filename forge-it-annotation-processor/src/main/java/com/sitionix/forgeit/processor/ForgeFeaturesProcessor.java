package com.sitionix.forgeit.processor;

import com.google.auto.service.AutoService;
import com.sitionix.forgeit.core.annotation.ForgeFeatures;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

@SupportedAnnotationTypes("com.sitionix.forgeit.core.annotation.ForgeFeatures")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public final class ForgeFeaturesProcessor extends AbstractProcessor {
    private static final ClassName GENERATED = ClassName.get("javax.annotation.processing", "Generated");

    private final Set<String> generatedTypes = new LinkedHashSet<>();

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotated : roundEnv.getElementsAnnotatedWith(ForgeFeatures.class)) {
            if (annotated.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures can only be applied to interfaces", annotated);
                continue;
            }

            TypeElement blueprint = (TypeElement) annotated;
            ForgeFeatures forgeFeatures = blueprint.getAnnotation(ForgeFeatures.class);
            if (forgeFeatures == null) {
                continue;
            }

            List<? extends TypeMirror> featureTypes = extractFeatureTypes(forgeFeatures);
            if (featureTypes.isEmpty()) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures must declare at least one feature", annotated);
                continue;
            }

            String exposedName = forgeFeatures.exposedName();
            if (exposedName == null || exposedName.isBlank()) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures requires 'exposedName' to generate a concrete interface", annotated);
                continue;
            }

            PackageElement pkg = elementUtils.getPackageOf(blueprint);
            String packageName = pkg.isUnnamed() ? "" : pkg.getQualifiedName().toString();
            String qualifiedName = packageName.isEmpty() ? exposedName : packageName + "." + exposedName;

            if (!generatedTypes.add(qualifiedName)) {
                continue;
            }

            try {
                generateInterface(blueprint, featureTypes, exposedName, packageName, qualifiedName);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return false;
    }

    private void generateInterface(TypeElement blueprint,
                                   List<? extends TypeMirror> featureTypes,
                                   String simpleName,
                                   String packageName,
                                   String qualifiedName) throws IOException {
        LinkedHashSet<String> seenInterfaces = new LinkedHashSet<>();
        List<TypeName> superInterfaces = new ArrayList<>();

        collectDeclaredInterfaces(blueprint, seenInterfaces, superInterfaces);
        collectFeatureInterfaces(featureTypes, blueprint, seenInterfaces, superInterfaces);

        TypeSpec.Builder typeBuilder = TypeSpec.interfaceBuilder(simpleName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(GENERATED)
                        .addMember("value", "$S", ForgeFeaturesProcessor.class.getName())
                        .build());

        superInterfaces.forEach(typeBuilder::addSuperinterface);

        JavaFile javaFile = JavaFile.builder(packageName, typeBuilder.build())
                .skipJavaLangImports(true)
                .build();

        JavaFileObject sourceFile = filer.createSourceFile(qualifiedName, blueprint);
        try (Writer writer = sourceFile.openWriter()) {
            javaFile.writeTo(writer);
        }
    }

    private void collectDeclaredInterfaces(TypeElement blueprint,
                                           Set<String> seenInterfaces,
                                           List<TypeName> superInterfaces) {
        for (TypeMirror mirror : blueprint.getInterfaces()) {
            TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(mirror);
            if (typeElement == null) {
                continue;
            }

            String fqName = typeElement.getQualifiedName().toString();
            if (seenInterfaces.add(fqName)) {
                superInterfaces.add(TypeName.get(mirror));
            }
        }
    }

    private void collectFeatureInterfaces(List<? extends TypeMirror> featureTypes,
                                          TypeElement blueprint,
                                          Set<String> seenInterfaces,
                                          List<TypeName> superInterfaces) {
        for (TypeMirror featureMirror : featureTypes) {
            TypeElement featureElement = (TypeElement) processingEnv.getTypeUtils().asElement(featureMirror);
            if (featureElement == null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Unable to resolve feature type", blueprint);
                continue;
            }

            String featureName = featureElement.getQualifiedName().toString();
            for (String supportTypeName : FeatureRegistry.resolveSupportInterfaces(featureName)) {
                TypeElement supportElement = elementUtils.getTypeElement(supportTypeName);
                if (supportElement == null) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Unknown feature support type: " + supportTypeName, blueprint);
                    continue;
                }

                String fqName = supportElement.getQualifiedName().toString();
                if (seenInterfaces.add(fqName)) {
                    superInterfaces.add(TypeName.get(supportElement.asType()));
                }
            }
        }
    }

    private List<? extends TypeMirror> extractFeatureTypes(ForgeFeatures annotation) {
        try {
            annotation.value();
        } catch (MirroredTypesException ex) {
            return new ArrayList<>(ex.getTypeMirrors());
        }
        return List.of();
    }
}
