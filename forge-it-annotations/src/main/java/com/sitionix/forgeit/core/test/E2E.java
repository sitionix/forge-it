package com.sitionix.forgeit.core.test;

import com.sitionix.forgeit.core.internal.test.E2eTestConfiguration;
import com.sitionix.forgeit.core.internal.test.ForgeItContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.lang.annotation.*;

/** Tests an already running environment using explicitly bound service contracts. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringJUnitConfig(E2eTestConfiguration.class)
@ActiveProfiles("e2e")
@TestExecutionListeners(listeners = ForgeItContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public @interface E2E {
    String[] properties() default {};
}
