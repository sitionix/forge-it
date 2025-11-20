package com.sitionix.forgeit.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

final class FeatureContractCollector {

    private final Messager messager;
    private final Set<String> aggregatedSupports = new LinkedHashSet<>();
    private final Set<String> processedContracts = new LinkedHashSet<>();

    FeatureContractCollector(Messager messager) {
        this.messager = messager;
    }

    void collect(TypeElement featureElement, Element sourceElement, Function<TypeMirror, TypeElement> typeResolver) {
        if (featureElement.getKind() != ElementKind.INTERFACE) {
            this.messager.printMessage(Diagnostic.Kind.ERROR,
                    "@ForgeFeatures values must be interfaces", sourceElement);
            return;
        }

        final String featureName = featureElement.getQualifiedName().toString();
        if (!this.processedContracts.add(featureName)) {
            return;
        }

        this.aggregatedSupports.add(featureName);

        for (final TypeMirror parentInterface : featureElement.getInterfaces()) {
            final TypeElement parentElement = typeResolver.apply(parentInterface);
            if (parentElement != null) {
                collect(parentElement, sourceElement, typeResolver);
            } else {
                this.aggregatedSupports.add(parentInterface.toString());
            }
        }
    }

    Set<String> getAggregatedSupports() {
        return Collections.unmodifiableSet(this.aggregatedSupports);
    }
}
