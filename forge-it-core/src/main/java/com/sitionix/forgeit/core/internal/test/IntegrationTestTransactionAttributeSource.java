package com.sitionix.forgeit.core.internal.test;

import com.sitionix.forgeit.core.test.IntegrationTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

import java.lang.reflect.Method;

/**
 * Ensures {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
 * can resolve a transaction attribute for {@link IntegrationTest} classes by plugging into the
 * {@link org.springframework.transaction.interceptor.TransactionAttributeSource} lookup that the
 * listener performs via {@code TestContextTransactionUtils}.
 */
final class IntegrationTestTransactionAttributeSource extends AnnotationTransactionAttributeSource {

    private static final TransactionAttribute INTEGRATION_TEST_ATTRIBUTE = createDefaultAttribute();

    IntegrationTestTransactionAttributeSource() {
        super(false);
    }

    @Override
    @Nullable
    public TransactionAttribute getTransactionAttribute(final Method method, @Nullable final Class<?> targetClass) {
        final TransactionAttribute attribute = super.getTransactionAttribute(method, targetClass);
        if (attribute != null || targetClass == null) {
            return attribute;
        }
        if (AnnotatedElementUtils.hasAnnotation(targetClass, IntegrationTest.class)) {
            return INTEGRATION_TEST_ATTRIBUTE;
        }
        return null;
    }

    private static TransactionAttribute createDefaultAttribute() {
        final DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setQualifier("transactionManager");
        return attribute;
    }
}
