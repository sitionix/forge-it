package com.sitionix.forgeit.processor;

import com.google.auto.service.AutoService;
import com.sitionix.forgeit.core.annotation.ForgeFeatures;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@SupportedAnnotationTypes("com.sitionix.forgeit.core.annotation.ForgeFeatures")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public final class ForgeFeaturesProcessor extends AbstractProcessor {
    private static final String FORGE_IT_FQN = "com.sitionix.forgeit.core.api.ForgeIT";

    private Messager messager;
    private Elements elements;
    private Types types;
    private Trees trees;
    private TreeMaker treeMaker;
    private Names names;
    private final Set<String> pendingSupports = new LinkedHashSet<>();
    private boolean forgeItAugmented;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.trees = Trees.instance(processingEnv);

        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
        this.treeMaker = TreeMaker.instance(javacEnv.getContext());
        this.names = Names.instance(javacEnv.getContext());
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
            if (!extendsForgeIT(interfaceElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures interfaces must extend ForgeIT", element);
                continue;
            }

            ForgeFeatures forgeFeatures = interfaceElement.getAnnotation(ForgeFeatures.class);
            Collection<? extends TypeMirror> featureTypes = extractFeatureTypes(forgeFeatures);
            if (featureTypes.isEmpty()) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@ForgeFeatures must declare at least one feature", element);
                continue;
            }

            for (TypeMirror featureMirror : featureTypes) {
                TypeElement featureElement = (TypeElement) types.asElement(featureMirror);
                if (featureElement == null) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Unable to resolve feature type", element);
                    continue;
                }

                for (String support : FeatureRegistry.resolveSupportInterfaces(featureElement.getQualifiedName().toString())) {
                    pendingSupports.add(support);
                }
            }
        }

        if (!pendingSupports.isEmpty() && !forgeItAugmented) {
            augmentForgeIT();
        }

        return false;
    }

    private Collection<? extends TypeMirror> extractFeatureTypes(ForgeFeatures annotation) {
        try {
            annotation.value();
        } catch (MirroredTypesException ex) {
            return ex.getTypeMirrors();
        }
        return java.util.List.of();
    }

    private boolean extendsForgeIT(TypeElement typeElement) {
        TypeElement forgeIT = elements.getTypeElement(FORGE_IT_FQN);
        if (forgeIT == null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "ForgeIT type was not found on the compilation classpath");
            return false;
        }

        TypeMirror forgeItMirror = forgeIT.asType();
        for (TypeMirror iface : typeElement.getInterfaces()) {
            if (types.isSameType(iface, forgeItMirror)) {
                return true;
            }

            if (iface instanceof DeclaredType declared) {
                Element element = declared.asElement();
                if (element instanceof TypeElement parent && extendsForgeIT(parent)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void augmentForgeIT() {
        TypeElement forgeIT = elements.getTypeElement(FORGE_IT_FQN);
        if (forgeIT == null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "ForgeIT type was not found on the compilation classpath");
            return;
        }

        TreePath treePath = trees.getPath(forgeIT);
        if (treePath == null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Unable to resolve the source tree for ForgeIT");
            return;
        }

        JCTree tree = (JCTree) treePath.getLeaf();
        if (!(tree instanceof JCTree.JCClassDecl classDecl)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "ForgeIT is not represented as a class declaration in the AST");
            return;
        }

        Set<String> alreadyPresent = new LinkedHashSet<>();
        for (JCTree.JCExpression existing : classDecl.implementing) {
            alreadyPresent.add(existing.toString());
        }

        ListBuffer<JCTree.JCExpression> additions = new ListBuffer<>();
        for (String support : pendingSupports) {
            if (alreadyPresent.contains(support)) {
                continue;
            }
            additions.append(qualify(support));
        }

        if (!additions.isEmpty()) {
            classDecl.implementing = classDecl.implementing.appendList(additions.toList());
        }

        forgeItAugmented = true;
        pendingSupports.clear();
    }

    private JCTree.JCExpression qualify(String fqcn) {
        String[] parts = fqcn.split("\\.");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Empty type name");
        }

        JCTree.JCExpression expression = treeMaker.Ident(name(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            expression = treeMaker.Select(expression, name(parts[i]));
        }
        return expression;
    }

    private Name name(String identifier) {
        Objects.requireNonNull(identifier, "identifier");
        return names.fromString(identifier);
    }
}
