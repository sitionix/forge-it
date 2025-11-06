package com.sitionix.forgeit.core.annotation;



import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ForgeFeatures {
    Class<?>[] value();

    /**
     * Name of the concrete interface that should be generated for the annotated blueprint.
     * When left blank the processor will report an error to avoid clashing with the
     * user-defined type compiled from source.
     */
    String exposedName() default "";
}
