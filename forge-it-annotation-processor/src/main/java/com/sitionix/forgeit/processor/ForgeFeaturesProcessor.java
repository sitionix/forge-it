package com.sitionix.forgeit.processor;

import com.google.auto.service.AutoService;
import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("com.sitionix.forgeit.core.annotation.ForgeFeatures")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public final class ForgeFeaturesProcessor extends AbstractProcessor {
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(javax.annotation.processing.ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(ForgeFeatures.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures can only be applied to interfaces", element);
                continue;
            }

            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures cannot be applied to private interfaces", element);
                continue;
            }

            TypeElement interfaceElement = (TypeElement) element;
            ForgeFeatures forgeFeatures = interfaceElement.getAnnotation(ForgeFeatures.class);
            List<? extends TypeMirror> featureTypes = extractFeatureTypes(forgeFeatures);
            if (featureTypes.isEmpty()) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures must declare at least one feature", element);
                continue;
            }

            String exposedName = forgeFeatures.exposedName();
            if (exposedName.isBlank()) {
                exposedName = interfaceElement.getSimpleName() + "Generated";
            } else if (exposedName.equals(interfaceElement.getSimpleName().toString())) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "exposedName must differ from the annotated interface name", element);
                continue;
            }

            generateFeatureInterface(interfaceElement, exposedName, featureTypes);
        }

        return false;
    }

    private void generateFeatureInterface(TypeElement blueprint,
                                          String exposedName,
                                          List<? extends TypeMirror> featureTypes) {
        PackageElement packageElement = elementUtils.getPackageOf(blueprint);
        String packageName = packageElement.isUnnamed() ? "" : packageElement.getQualifiedName().toString();

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(exposedName)
                .addModifiers(Modifier.PUBLIC)
                .addOriginatingElement(blueprint);

        for (TypeParameterElement parameter : blueprint.getTypeParameters()) {
            interfaceBuilder.addTypeVariable(TypeVariableName.get(parameter));
        }

        Set<String> alreadyAdded = new LinkedHashSet<>();
        for (TypeMirror parent : blueprint.getInterfaces()) {
            TypeElement parentElement = (TypeElement) typeUtils.asElement(parent);
            if (parentElement != null) {
                String qualifiedName = parentElement.getQualifiedName().toString();
                if (alreadyAdded.add(qualifiedName)) {
                    interfaceBuilder.addSuperinterface(TypeName.get(parent));
                }
            } else {
                TypeName parentType = TypeName.get(parent);
                if (alreadyAdded.add(parentType.toString())) {
                    interfaceBuilder.addSuperinterface(parentType);
                }
            }
        }

        copyBlueprintMethods(blueprint, interfaceBuilder);

        for (TypeMirror featureMirror : featureTypes) {
            TypeElement featureElement = (TypeElement) typeUtils.asElement(featureMirror);
            if (featureElement == null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Unable to resolve feature type", blueprint);
                continue;
            }

            for (String supportType : FeatureRegistry.resolveSupportInterfaces(featureElement.getQualifiedName().toString())) {
                if (alreadyAdded.add(supportType)) {
                    interfaceBuilder.addSuperinterface(ClassName.bestGuess(supportType));
                }
            }
        }

        JavaFile javaFile = JavaFile.builder(packageName, interfaceBuilder.build())
                .skipJavaLangImports(true)
                .build();

        try {
            javaFile.writeTo(filer);
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate interface '" + exposedName + "': " + ex.getMessage(), blueprint);
        }
    }

    private List<? extends TypeMirror> extractFeatureTypes(ForgeFeatures annotation) {
        try {
            annotation.value();
        } catch (MirroredTypesException ex) {
            return ex.getTypeMirrors();
        }
        return List.of();
    }

    private void copyBlueprintMethods(TypeElement blueprint, TypeSpec.Builder interfaceBuilder) {
        for (Element enclosed : blueprint.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) {
                continue;
            }

            ExecutableElement method = (ExecutableElement) enclosed;

            if (method.getModifiers().contains(Modifier.DEFAULT)
                    || method.getModifiers().contains(Modifier.STATIC)
                    || method.getModifiers().contains(Modifier.PRIVATE)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures does not support default, static, or private interface methods", method);
                continue;
            }

            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(TypeName.get(method.getReturnType()));

            for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
                methodBuilder.addTypeVariable(TypeVariableName.get(typeParameterElement));
            }

            for (VariableElement parameter : method.getParameters()) {
                ParameterSpec parameterSpec = ParameterSpec.builder(
                                TypeName.get(parameter.asType()),
                                parameter.getSimpleName().toString())
                        .build();
                methodBuilder.addParameter(parameterSpec);
            }

            for (TypeMirror thrownType : method.getThrownTypes()) {
                methodBuilder.addException(TypeName.get(thrownType));
            }

            interfaceBuilder.addMethod(methodBuilder.build());
        }
    }
}
