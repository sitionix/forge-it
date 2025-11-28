package com.sitionix.forgeit.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;

final class GeneratedApiImplEmitter {

    private final ProcessingEnvironment processingEnv;
    private final Messager messager;

    GeneratedApiImplEmitter(ProcessingEnvironment processingEnv, Messager messager) {
        this.processingEnv = processingEnv;
        this.messager = messager;
    }

    void generateImplementation(TypeElement apiInterface) {
        final String packageName = this.processingEnv.getElementUtils()
                .getPackageOf(apiInterface)
                .getQualifiedName()
                .toString();

        final String interfaceSimpleName = apiInterface.getSimpleName().toString();
        final String implSimpleName = interfaceSimpleName + "Impl";

        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(implSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(apiInterface))
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Component"));

        final JavaFile javaFile = JavaFile.builder(packageName, typeBuilder.build())
                .skipJavaLangImports(true)
                .build();

        try {
            final JavaFileObject sourceFile = this.processingEnv.getFiler()
                    .createSourceFile(packageName + "." + implSimpleName, apiInterface);

            try (Writer writer = sourceFile.openWriter()) {
                javaFile.writeTo(writer);
            }
        } catch (FilerException ex) {
            this.messager.printMessage(Diagnostic.Kind.NOTE,
                    "Implementation already exists for " + apiInterface.getQualifiedName()
                            + ": " + ex.getMessage());
        } catch (IOException ex) {
            this.messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate ForgeIT API implementation for "
                            + apiInterface.getQualifiedName() + ": " + ex.getMessage());
        }
    }
}
