package com.sitionix.forgeit.application.executor;

public final class TestRollbackContextHolder {

    private static final ThreadLocal<Class<?>> CURRENT_TEST_CLASS = new ThreadLocal<>();

    private TestRollbackContextHolder() {
    }

    public static void setCurrentTestClass(final Class<?> testClass) {
        CURRENT_TEST_CLASS.set(testClass);
    }

    public static Class<?> getCurrentTestClass() {
        return CURRENT_TEST_CLASS.get();
    }

    public static void clear() {
        CURRENT_TEST_CLASS.remove();
    }
}
