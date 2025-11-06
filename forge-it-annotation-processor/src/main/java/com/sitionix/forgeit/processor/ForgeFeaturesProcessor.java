package com.sitionix.forgeit.processor;

import com.google.auto.service.AutoService;
import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacProcessingEnvironment;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("com.sitionix.forgeit.core.annotation.ForgeFeatures")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public final class ForgeFeaturesProcessor extends AbstractProcessor {
    private Messager messager;
    private com.sun.tools.javac.api.JavacTrees trees;
    private TreeMaker treeMaker;
    private Types typeUtils;
    private Elements elementUtils;
    private final Set<String> processedTypes = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.typeUtils = processingEnv.getTypeUtils();
        this.elementUtils = processingEnv.getElementUtils();
        this.trees = com.sun.tools.javac.api.JavacTrees.instance(processingEnv);

        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
        this.treeMaker = TreeMaker.instance(javacEnv.getContext());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(ForgeFeatures.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Diagnostic.Kind.ERROR, "@ForgeFeatures can only be applied to interfaces", element);
                continue;
            }

            TypeElement typeElement = (TypeElement) element;
            String typeName = typeElement.getQualifiedName().toString();
            if (!processedTypes.add(typeName)) {
                continue;
            }

            List<? extends TypeMirror> featureTypes = extractFeatureTypes(typeElement);
            if (featureTypes.isEmpty()) {
                messager.printMessage(Diagnostic.Kind.ERROR, "@ForgeFeatures must declare at least one feature", element);
                continue;
            }

            JCTree tree = trees.getTree(typeElement);
            if (!(tree instanceof JCTree.JCClassDecl classDecl)) {
                messager.printMessage(Diagnostic.Kind.WARNING, "Unable to read class declaration for " + typeName, element);
                continue;
            }

            Set<String> alreadyImplemented = new HashSet<>();
            TreePath typePath = trees.getPath(typeElement);
            for (JCTree.JCExpression implemented : classDecl.implementing) {
                alreadyImplemented.add(implemented.toString());
                if (typePath != null) {
                    TreePath implPath = TreePath.getPath(typePath, implemented);
                    Element resolved = implPath != null ? trees.getElement(implPath) : null;
                    if (resolved instanceof TypeElement resolvedType) {
                        alreadyImplemented.add(resolvedType.getQualifiedName().toString());
                    }
                }
            }

            ListBuffer<JCTree.JCExpression> updatedImplementing = new ListBuffer<>();
            for (JCTree.JCExpression implemented : classDecl.implementing) {
                updatedImplementing.append(implemented);
            }

            for (TypeMirror mirror : featureTypes) {
                TypeElement featureElement = (TypeElement) typeUtils.asElement(mirror);
                if (featureElement == null) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Unable to resolve feature type", element);
                    continue;
                }
                String featureName = featureElement.getQualifiedName().toString();
                for (String supportTypeName : FeatureRegistry.resolveSupportInterfaces(featureName)) {
                    if (alreadyImplemented.contains(supportTypeName)) {
                        continue;
                    }

                    TypeElement supportElement = supportTypeName.equals(featureName)
                            ? featureElement
                            : elementUtils.getTypeElement(supportTypeName);
                    if (supportElement == null) {
                        messager.printMessage(Diagnostic.Kind.ERROR,
                                "Unknown feature support type: " + supportTypeName, element);
                        continue;
                    }

                    Symbol.TypeSymbol symbol = (Symbol.TypeSymbol) supportElement;
                    JCTree.JCExpression featureExpr = treeMaker.QualIdent(symbol);
                    updatedImplementing.append(featureExpr);
                    alreadyImplemented.add(supportTypeName);
                }
            }

            classDecl.implementing = updatedImplementing.toList();
        }
        return false;
    }

    private List<? extends TypeMirror> extractFeatureTypes(TypeElement element) {
        ForgeFeatures annotation = element.getAnnotation(ForgeFeatures.class);
        if (annotation == null) {
            return List.of();
        }
        try {
            annotation.value();
        } catch (MirroredTypesException ex) {
            return new ArrayList<>(ex.getTypeMirrors());
        }
        return List.of();
    }
}
