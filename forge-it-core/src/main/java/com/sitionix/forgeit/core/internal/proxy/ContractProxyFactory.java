package com.sitionix.forgeit.core.internal.proxy;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class ContractProxyFactory {

    private ContractProxyFactory() {
    }

    public static void registerContractProxy(ConfigurableApplicationContext context, Class<?> contractType) {
        final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        final String beanName = contractType.getName();
        if (beanFactory.containsSingleton(beanName)) {
            return;
        }
        final Object proxy = createContractProxy(context, contractType);
        beanFactory.registerSingleton(beanName, proxy);
    }

    private static Object createContractProxy(ConfigurableApplicationContext context, Class<?> contractType) {
        final ClassLoader classLoader = context.getClassLoader();
        return Proxy.newProxyInstance(classLoader, new Class<?>[]{contractType},
                new ContractInvocationHandler(contractType));
    }

    private static final class ContractInvocationHandler implements InvocationHandler {

        private final Class<?> contractType;

        private ContractInvocationHandler(Class<?> contractType) {
            this.contractType = contractType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isDefault()) {
                return invokeDefaultMethod(proxy, method, args);
            }
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }
            throw new UnsupportedOperationException(
                    "No implementation available for method " + method.toGenericString());
        }

        private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
            final String name = method.getName();
            return switch (name) {
                case "toString" -> this.contractType.getName() + " proxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
                default -> throw new IllegalStateException("Unexpected Object method: " + name);
            };
        }

        private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
            final Class<?> declaringClass = method.getDeclaringClass();
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup());
            final MethodHandle handle = lookup.findSpecial(
                    declaringClass,
                    method.getName(),
                    MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                    declaringClass)
                    .bindTo(proxy);
            return handle.invokeWithArguments(args == null ? new Object[0] : args);
        }
    }
}
