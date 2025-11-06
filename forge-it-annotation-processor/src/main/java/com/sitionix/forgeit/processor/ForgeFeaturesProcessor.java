package com.sitionix.forgeit.processor;

import com.google.auto.service.AutoService;
import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

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
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.LinkedHashSet;
import java.util.Set;

@SupportedAnnotationTypes("com.sitionix.forgeit.core.annotation.ForgeFeatures")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public final class ForgeFeaturesProcessor extends AbstractProcessor {
    private Messager messager;
    private Elements elementUtils;
    private javax.lang.model.util.Types typeUtils;
    private JavacProcessingEnvironment javacEnv;
    private JavacTrees trees;
    private TreeMaker treeMaker;
    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.javacEnv = (JavacProcessingEnvironment) processingEnv;
        this.trees = (JavacTrees) Trees.instance(processingEnv);
        this.treeMaker = TreeMaker.instance(javacEnv.getContext());
        this.types = Types.instance(javacEnv.getContext());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotated : roundEnv.getElementsAnnotatedWith(ForgeFeatures.class)) {
            if (annotated.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures can only be applied to interfaces", annotated);
                continue;
            }

            if (annotated.getModifiers().contains(Modifier.PRIVATE)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures cannot be placed on private interfaces", annotated);
                continue;
            }

            TypeElement interfaceElement = (TypeElement) annotated;
            ForgeFeatures forgeFeatures = interfaceElement.getAnnotation(ForgeFeatures.class);
            java.util.List<? extends TypeMirror> featureTypes = extractFeatureTypes(forgeFeatures);
            if (featureTypes.isEmpty()) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures must declare at least one feature", annotated);
                continue;
            }

            JCTree tree = trees.getTree(interfaceElement);
            if (!(tree instanceof JCClassDecl classDecl)) {
                continue;
            }

            augmentInterface(interfaceElement, classDecl, featureTypes);
        }
        return false;
    }

    private void augmentInterface(TypeElement interfaceElement,
                                  JCClassDecl classDecl,
                                  java.util.List<? extends TypeMirror> featureTypes) {
        Set<String> seenInterfaces = collectExistingInterfaces(interfaceElement, classDecl);

        ListBuffer<JCExpression> newSuperInterfaces = new ListBuffer<>();
        ListBuffer<Type> newInterfaceTypes = new ListBuffer<>();

        for (TypeMirror featureMirror : featureTypes) {
            TypeElement featureElement = (TypeElement) typeUtils.asElement(featureMirror);
            if (featureElement == null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Unable to resolve feature type", interfaceElement);
                continue;
            }

            for (String supportName : FeatureRegistry.resolveSupportInterfaces(featureElement.getQualifiedName().toString())) {
                TypeElement supportElement = elementUtils.getTypeElement(supportName);
                if (supportElement == null) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Unknown feature support type: " + supportName, interfaceElement);
                    continue;
                }

                String qualifiedName = supportElement.getQualifiedName().toString();
                if (!seenInterfaces.add(qualifiedName)) {
                    continue;
                }

                Symbol.ClassSymbol supportSymbol = (Symbol.ClassSymbol) supportElement;
                newSuperInterfaces.append(treeMaker.QualIdent(supportSymbol));
                newInterfaceTypes.append(supportSymbol.type);
            }
        }

        if (newSuperInterfaces.isEmpty()) {
            return;
        }

        classDecl.implementing = classDecl.implementing.appendList(newSuperInterfaces.toList());

        Symbol.ClassSymbol interfaceSymbol = (Symbol.ClassSymbol) interfaceElement;
        ClassType classType = (ClassType) interfaceSymbol.type;
        List<Type> additionalTypes = newInterfaceTypes.toList();

        List<Type> currentClassInterfaces = classType.interfaces_field == null ? List.nil() : classType.interfaces_field;
        classType.interfaces_field = currentClassInterfaces.appendList(additionalTypes);
        classType.all_interfaces_field = types.closure(classType);

        List<Type> currentSymbolInterfaces = interfaceSymbol.interfaces_field == null ? List.nil() : interfaceSymbol.interfaces_field;
        interfaceSymbol.interfaces_field = currentSymbolInterfaces.appendList(additionalTypes);
    }

    private Set<String> collectExistingInterfaces(TypeElement interfaceElement, JCClassDecl classDecl) {
        Set<String> seen = new LinkedHashSet<>();
        for (TypeMirror mirror : interfaceElement.getInterfaces()) {
            TypeElement typeElement = (TypeElement) typeUtils.asElement(mirror);
            if (typeElement != null) {
                seen.add(typeElement.getQualifiedName().toString());
            }
        }
        for (JCExpression expression : classDecl.implementing) {
            seen.add(expression.toString());
        }
        return seen;
    }

    private java.util.List<? extends TypeMirror> extractFeatureTypes(ForgeFeatures annotation) {
        try {
            annotation.value();
        } catch (MirroredTypesException ex) {
            return ex.getTypeMirrors();
        }
        return java.util.List.of();
    }
}
