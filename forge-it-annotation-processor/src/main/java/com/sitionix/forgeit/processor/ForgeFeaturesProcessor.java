package com.sitionix.forgeit.processor;

import com.sitionix.forgeit.core.annotation.ForgeFeatures;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes("com.sitionix.forgeit.core.api.ForgeFeatures")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class ForgeFeaturesProcessor extends AbstractProcessor {
    private Messager msg;

    @Override public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        msg = env.getMessager();
    }

    @Override public boolean process(Set<? extends TypeElement> ann, RoundEnvironment round) {
        for (Element el : round.getElementsAnnotatedWith(ForgeFeatures.class)) {
            if (el.getKind() != ElementKind.INTERFACE) {
                msg.printMessage(Diagnostic.Kind.ERROR, "@ForgeFeatures only on interfaces", el);
                continue;
            }
            ForgeFeatures ff = el.getAnnotation(ForgeFeatures.class);
            if (ff == null || ff.value().length == 0) {
                msg.printMessage(Diagnostic.Kind.ERROR, "@ForgeFeatures must include at least one feature", el);
                continue;
            }
            for (Class<?> c : ff.value()) {
                String fqn = c.getCanonicalName();
                if (!FeatureRegistry.FEATURE_TO_SUPPORT.containsKey(fqn)) {
                    msg.printMessage(Diagnostic.Kind.ERROR, "Unknown feature/support: " + fqn, el);
                }
            }
        }
        return false;
    }
}
