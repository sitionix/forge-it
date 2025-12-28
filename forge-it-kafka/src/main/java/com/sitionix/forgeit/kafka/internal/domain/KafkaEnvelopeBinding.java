package com.sitionix.forgeit.kafka.internal.domain;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class KafkaEnvelopeBinding {

    private KafkaEnvelopeBinding() {
    }

    static <E> E createEnvelope(final Class<E> envelopeType) {
        if (envelopeType == null) {
            throw new IllegalArgumentException("envelopeType must be provided");
        }
        try {
            final Constructor<E> constructor = envelopeType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final NoSuchMethodException ex) {
            throw new IllegalStateException("Envelope type " + envelopeType.getName() +
                    " must declare a no-args constructor", ex);
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to instantiate envelope type " + envelopeType.getName(), ex);
        }
    }

    static void injectPayload(final Object envelope, final Object payload, final Class<?> payloadType) {
        if (payload == null || payloadType == null) {
            return;
        }
        final ValueWriter writer = resolveWriter(envelope.getClass(), payloadType, "payload");
        writer.write(envelope, payload);
    }

    static void injectMetadata(final Object envelope, final Object metadata, final Class<?> metadataType) {
        if (metadata == null || metadataType == null) {
            return;
        }
        final ValueWriter writer = resolveWriter(envelope.getClass(), metadataType, "metadata");
        writer.write(envelope, metadata);
    }

    static Object extractPayload(final Object envelope, final Class<?> payloadType) {
        if (payloadType == null) {
            return null;
        }
        final ValueReader reader = resolveReader(envelope.getClass(), payloadType, "payload");
        return reader.read(envelope);
    }

    static Object extractMetadata(final Object envelope, final Class<?> metadataType) {
        if (metadataType == null) {
            return null;
        }
        final ValueReader reader = resolveReader(envelope.getClass(), metadataType, "metadata");
        return reader.read(envelope);
    }

    private static ValueWriter resolveWriter(final Class<?> envelopeType,
                                             final Class<?> valueType,
                                             final String label) {
        final Method exactSetter = selectSingleMethod(findSetterCandidates(envelopeType, valueType, true),
                envelopeType,
                label,
                "setter");
        if (exactSetter != null) {
            return new MethodWriter(exactSetter);
        }
        final Method assignableSetter = selectSingleMethod(findSetterCandidates(envelopeType, valueType, false),
                envelopeType,
                label,
                "setter");
        if (assignableSetter != null) {
            return new MethodWriter(assignableSetter);
        }

        final Field exactField = selectSingleField(findFieldCandidates(envelopeType, valueType, true),
                envelopeType,
                label);
        if (exactField != null) {
            return new FieldWriter(exactField);
        }
        final Field assignableField = selectSingleField(findFieldCandidates(envelopeType, valueType, false),
                envelopeType,
                label);
        if (assignableField != null) {
            return new FieldWriter(assignableField);
        }

        throw new IllegalStateException("Envelope type " + envelopeType.getName()
                + " does not expose a " + label + " setter or field compatible with " + valueType.getName());
    }

    private static ValueReader resolveReader(final Class<?> envelopeType,
                                             final Class<?> valueType,
                                             final String label) {
        final Method exactGetter = selectSingleMethod(findGetterCandidates(envelopeType, valueType, true),
                envelopeType,
                label,
                "getter");
        if (exactGetter != null) {
            return new MethodReader(exactGetter);
        }
        final Method assignableGetter = selectSingleMethod(findGetterCandidates(envelopeType, valueType, false),
                envelopeType,
                label,
                "getter");
        if (assignableGetter != null) {
            return new MethodReader(assignableGetter);
        }

        final Field exactField = selectSingleField(findFieldCandidates(envelopeType, valueType, true),
                envelopeType,
                label);
        if (exactField != null) {
            return new FieldReader(exactField);
        }
        final Field assignableField = selectSingleField(findFieldCandidates(envelopeType, valueType, false),
                envelopeType,
                label);
        if (assignableField != null) {
            return new FieldReader(assignableField);
        }

        throw new IllegalStateException("Envelope type " + envelopeType.getName()
                + " does not expose a " + label + " getter or field compatible with " + valueType.getName());
    }

    private static List<Method> findSetterCandidates(final Class<?> type,
                                                     final Class<?> valueType,
                                                     final boolean exactMatch) {
        final List<Method> candidates = new ArrayList<>();
        Class<?> current = type;
        while (current != null && !Object.class.equals(current)) {
            for (final Method method : current.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() != 1) {
                    continue;
                }
                final Class<?> paramType = method.getParameterTypes()[0];
                if (!matchesType(paramType, valueType, exactMatch)) {
                    continue;
                }
                method.setAccessible(true);
                candidates.add(method);
            }
            current = current.getSuperclass();
        }
        candidates.sort(Comparator.comparing(Method::getName));
        return candidates;
    }

    private static List<Method> findGetterCandidates(final Class<?> type,
                                                     final Class<?> valueType,
                                                     final boolean exactMatch) {
        final List<Method> candidates = new ArrayList<>();
        Class<?> current = type;
        while (current != null && !Object.class.equals(current)) {
            for (final Method method : current.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() != 0) {
                    continue;
                }
                final Class<?> returnType = method.getReturnType();
                if (!matchesReturnType(returnType, valueType, exactMatch)) {
                    continue;
                }
                method.setAccessible(true);
                candidates.add(method);
            }
            current = current.getSuperclass();
        }
        candidates.sort(Comparator.comparing(Method::getName));
        return candidates;
    }

    private static List<Field> findFieldCandidates(final Class<?> type,
                                                   final Class<?> valueType,
                                                   final boolean exactMatch) {
        final List<Field> candidates = new ArrayList<>();
        Class<?> current = type;
        while (current != null && !Object.class.equals(current)) {
            for (final Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!matchesType(field.getType(), valueType, exactMatch)) {
                    continue;
                }
                field.setAccessible(true);
                candidates.add(field);
            }
            current = current.getSuperclass();
        }
        candidates.sort(Comparator.comparing(Field::getName));
        return candidates;
    }

    private static Method selectSingleMethod(final List<Method> candidates,
                                             final Class<?> envelopeType,
                                             final String label,
                                             final String kind) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        throw new IllegalStateException("Envelope type " + envelopeType.getName() +
                " has multiple " + label + " " + kind + "s: " + describeMethods(candidates));
    }

    private static Field selectSingleField(final List<Field> candidates,
                                           final Class<?> envelopeType,
                                           final String label) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        throw new IllegalStateException("Envelope type " + envelopeType.getName() +
                " has multiple " + label + " fields: " + describeFields(candidates));
    }

    private static boolean matchesType(final Class<?> declaredType,
                                       final Class<?> valueType,
                                       final boolean exactMatch) {
        if (exactMatch) {
            return declaredType.equals(valueType);
        }
        return declaredType.isAssignableFrom(valueType);
    }

    private static boolean matchesReturnType(final Class<?> returnType,
                                             final Class<?> valueType,
                                             final boolean exactMatch) {
        if (exactMatch) {
            return returnType.equals(valueType);
        }
        return valueType.isAssignableFrom(returnType);
    }

    private static String describeMethods(final List<Method> methods) {
        final List<String> names = new ArrayList<>();
        for (final Method method : methods) {
            names.add(method.getName() + "(" + method.getParameterCount() + ")");
        }
        return String.join(", ", names);
    }

    private static String describeFields(final List<Field> fields) {
        final List<String> names = new ArrayList<>();
        for (final Field field : fields) {
            names.add(field.getName());
        }
        return String.join(", ", names);
    }

    private interface ValueWriter {
        void write(Object target, Object value);
    }

    private interface ValueReader {
        Object read(Object target);
    }

    private record MethodWriter(Method method) implements ValueWriter {
        @Override
        public void write(final Object target, final Object value) {
            try {
                this.method.invoke(target, value);
            } catch (final ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to invoke envelope setter " + this.method.getName(), ex);
            }
        }
    }

    private record FieldWriter(Field field) implements ValueWriter {
        @Override
        public void write(final Object target, final Object value) {
            try {
                this.field.set(target, value);
            } catch (final IllegalAccessException ex) {
                throw new IllegalStateException("Failed to set envelope field " + this.field.getName(), ex);
            }
        }
    }

    private record MethodReader(Method method) implements ValueReader {
        @Override
        public Object read(final Object target) {
            try {
                return this.method.invoke(target);
            } catch (final ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to invoke envelope getter " + this.method.getName(), ex);
            }
        }
    }

    private record FieldReader(Field field) implements ValueReader {
        @Override
        public Object read(final Object target) {
            try {
                return this.field.get(target);
            } catch (final IllegalAccessException ex) {
                throw new IllegalStateException("Failed to read envelope field " + this.field.getName(), ex);
            }
        }
    }
}
