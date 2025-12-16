package com.sitionix.forgeit.core.contract;

import com.sitionix.forgeit.domain.contract.clean.CleanupPhase;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DbCleanup {

    CleanupPhase phase() default CleanupPhase.NONE;
}
