package com.sitionix.forgeit.processor;

import com.google.auto.service.AutoService;
import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.squareup.javapoet.ClassName;

import javax.annotation.processing.AbstractProcessor;
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
import javax.tools.Diagnostic.Kind;
import java.util.Collection;
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

    private FeatureRegistry featureRegistry;
    private FeatureContractCollector featureContractCollector;
    private GeneratedInterfaceEmitter generatedInterfaceEmitter;
    private GeneratedApiImplEmitter generatedApiImplEmitter;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.featureRegistry = new FeatureRegistry(processingEnv, messager, elements);
        this.featureContractCollector = new FeatureContractCollector(messager);
        this.generatedInterfaceEmitter = new GeneratedInterfaceEmitter(processingEnv, messager, GENERATED_FEATURES);
        this.generatedApiImplEmitter = new GeneratedApiImplEmitter(processingEnv, messager);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (final Element element : roundEnv.getElementsAnnotatedWith(ForgeFeatures.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                this.messager.printMessage(Kind.ERROR, "@ForgeFeatures can only be applied to interfaces", element);
                continue;
            }

            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                this.messager.printMessage(Kind.ERROR, "@ForgeFeatures cannot be applied to private interfaces", element);
                continue;
            }

            final TypeElement interfaceElement = (TypeElement) element;
            if (!extendsForgeIT(interfaceElement)) {
                this.messager.printMessage(Kind.ERROR, "@ForgeFeatures interfaces must extend ForgeIT", element);
                continue;
            }

            final ForgeFeatures forgeFeatures = interfaceElement.getAnnotation(ForgeFeatures.class);
            final Collection<? extends TypeMirror> featureTypes = extractFeatureTypes(forgeFeatures);
            if (featureTypes.isEmpty()) {
                this.messager.printMessage(Kind.ERROR, "@ForgeFeatures must declare at least one feature", element);
                continue;
            }

            boolean hasInvalidFeature = false;
            for (final TypeMirror featureMirror : featureTypes) {
                final TypeElement featureElement = asTypeElement(featureMirror);
                if (featureElement == null) {
                    this.messager.printMessage(Kind.ERROR, "Unable to resolve feature type", element);
                    hasInvalidFeature = true;
                    continue;
                }

                if (!isFeatureSupport(featureElement)) {
                    this.messager.printMessage(Kind.ERROR,
                            "Each entry in @ForgeFeatures must extend FeatureSupport: " + featureElement.getQualifiedName(), element);
                    hasInvalidFeature = true;
                    continue;
                }
                if (!this.featureRegistry.isWhitelisted(featureElement)) {
                    this.messager.printMessage(Kind.ERROR,
                            "Feature is not registered. Add it to META-INF/forge-it/features: " + featureElement.getQualifiedName(), element);
                    hasInvalidFeature = true;
                    continue;
                }

                this.featureContractCollector.collect(featureElement, element, this::asTypeElement);
            }

            if (!hasInvalidFeature) {
                this.generatedApiImplEmitter.generateImplementation(interfaceElement);
            }
        }

        final Set<String> aggregatedSupports = this.featureContractCollector.getAggregatedSupports();
        if (!aggregatedSupports.isEmpty()) {
            this.generatedInterfaceEmitter.generateInterface(aggregatedSupports);
        }
        return false;
    }

    private boolean isFeatureSupport(TypeElement type) {
        final TypeElement featureSupport = this.elements.getTypeElement(FEATURE_SUPPORT_FQN);
        return featureSupport != null && this.types.isAssignable(type.asType(), featureSupport.asType());
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

        final TypeElement forgeIt = this.elements.getTypeElement(FORGE_IT_FQN);
        if (forgeIt == null) {
            this.messager.printMessage(Kind.ERROR, "ForgeIT type was not found on the compilation classpath");
            return false;
        }
        return implementsInterface(candidate, forgeIt.asType());
    }

    private boolean implementsInterface(TypeElement candidate, TypeMirror targetInterface) {
        for (final TypeMirror iface : candidate.getInterfaces()) {
            if (this.types.isSameType(iface, targetInterface)) {
                return true;
            }
            final Element element = this.types.asElement(iface);
            if (element instanceof TypeElement typeElement && implementsInterface(typeElement, targetInterface)) {
                return true;
            }
        }

        final TypeMirror superclass = candidate.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE) {
            final Element element = this.types.asElement(superclass);
            return element instanceof TypeElement typeElement && implementsInterface(typeElement, targetInterface);
        }
        return false;
    }

    private TypeElement asTypeElement(TypeMirror mirror) {
        if (mirror instanceof DeclaredType declaredType) {
            final Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                return typeElement;
            }
        }
        return null;
    }
}
