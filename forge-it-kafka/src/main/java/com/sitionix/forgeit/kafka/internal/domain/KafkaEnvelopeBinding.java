package com.sitionix.forgeit.kafka.internal.domain;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class KafkaEnvelopeBinding {

    private static final List<String> PAYLOAD_SETTER_NAMES = List.of("setPayload", "setEvent");
    private static final List<String> PAYLOAD_GETTER_NAMES = List.of("getPayload", "getEvent");
    private static final List<String> PAYLOAD_FIELD_NAMES = List.of("payload", "event");
    private static final List<String> METADATA_SETTER_NAMES = List.of("setMetadata");
    private static final List<String> METADATA_GETTER_NAMES = List.of("getMetadata");
    private static final List<String> METADATA_FIELD_NAMES = List.of("metadata");

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
        final ValueWriter writer = resolveWriter(envelope.getClass(),
                payloadType,
                PAYLOAD_SETTER_NAMES,
                PAYLOAD_FIELD_NAMES,
                "payload");
        writer.write(envelope, payload);
    }

    static void injectMetadata(final Object envelope, final Object metadata, final Class<?> metadataType) {
        if (metadata == null || metadataType == null) {
            return;
        }
        final ValueWriter writer = resolveWriter(envelope.getClass(),
                metadataType,
                METADATA_SETTER_NAMES,
                METADATA_FIELD_NAMES,
                "metadata");
        writer.write(envelope, metadata);
    }

    static Object extractPayload(final Object envelope, final Class<?> payloadType) {
        if (payloadType == null) {
            return null;
        }
        final ValueReader reader = resolveReader(envelope.getClass(),
                payloadType,
                PAYLOAD_GETTER_NAMES,
                PAYLOAD_FIELD_NAMES,
                "payload");
        return reader.read(envelope);
    }

    static Object extractMetadata(final Object envelope, final Class<?> metadataType) {
        if (metadataType == null) {
            return null;
        }
        final ValueReader reader = resolveReader(envelope.getClass(),
                metadataType,
                METADATA_GETTER_NAMES,
                METADATA_FIELD_NAMES,
                "metadata");
        return reader.read(envelope);
    }

    private static ValueWriter resolveWriter(final Class<?> envelopeType,
                                             final Class<?> valueType,
                                             final List<String> preferredMethodNames,
                                             final List<String> preferredFieldNames,
                                             final String label) {
        final List<Method> setters = findSetterCandidates(envelopeType, valueType);
        final Optional<Method> preferredSetter = selectPreferredMethod(setters, preferredMethodNames);
        if (preferredSetter.isPresent()) {
            return new MethodWriter(preferredSetter.get());
        }
        if (setters.size() == 1) {
            return new MethodWriter(setters.get(0));
        }
        if (setters.size() > 1) {
            throw new IllegalStateException("Envelope type " + envelopeType.getName() +
                    " has multiple " + label + " setters: " + describeMethods(setters));
        }

        final List<Field> fields = findReadableFieldCandidates(envelopeType, valueType);
        final Optional<Field> preferredField = selectPreferredField(fields, preferredFieldNames);
        if (preferredField.isPresent()) {
            return new FieldWriter(preferredField.get());
        }
        if (fields.size() == 1) {
            return new FieldWriter(fields.get(0));
        }
        if (fields.size() > 1) {
            throw new IllegalStateException("Envelope type " + envelopeType.getName() +
                    " has multiple " + label + " fields: " + describeFields(fields));
        }

        throw new IllegalStateException("Envelope type " + envelopeType.getName() +
                " does not expose a " + label + " setter or field compatible with " + valueType.getName());
    }

    private static ValueReader resolveReader(final Class<?> envelopeType,
                                             final Class<?> valueType,
                                             final List<String> preferredMethodNames,
                                             final List<String> preferredFieldNames,
                                             final String label) {
        final List<Method> getters = findGetterCandidates(envelopeType, valueType);
        final Optional<Method> preferredGetter = selectPreferredMethod(getters, preferredMethodNames);
        if (preferredGetter.isPresent()) {
            return new MethodReader(preferredGetter.get());
        }
        if (getters.size() == 1) {
            return new MethodReader(getters.get(0));
        }
        if (getters.size() > 1) {
            throw new IllegalStateException("Envelope type " + envelopeType.getName() +
                    " has multiple " + label + " getters: " + describeMethods(getters));
        }

        final List<Field> fields = findFieldCandidates(envelopeType, valueType);
        final Optional<Field> preferredField = selectPreferredField(fields, preferredFieldNames);
        if (preferredField.isPresent()) {
            return new FieldReader(preferredField.get());
        }
        if (fields.size() == 1) {
            return new FieldReader(fields.get(0));
        }
        if (fields.size() > 1) {
            throw new IllegalStateException("Envelope type " + envelopeType.getName() +
                    " has multiple " + label + " fields: " + describeFields(fields));
        }

        throw new IllegalStateException("Envelope type " + envelopeType.getName() +
                " does not expose a " + label + " getter or field compatible with " + valueType.getName());
    }

    private static List<Method> findSetterCandidates(final Class<?> type, final Class<?> valueType) {
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
                if (!paramType.isAssignableFrom(valueType)) {
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

    private static List<Method> findGetterCandidates(final Class<?> type, final Class<?> valueType) {
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
                if (!valueType.isAssignableFrom(returnType)) {
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

    private static List<Field> findFieldCandidates(final Class<?> type, final Class<?> valueType) {
        final List<Field> candidates = new ArrayList<>();
        Class<?> current = type;
        while (current != null && !Object.class.equals(current)) {
            for (final Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!field.getType().isAssignableFrom(valueType)) {
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

    private static List<Field> findReadableFieldCandidates(final Class<?> type, final Class<?> valueType) {
        final List<Field> candidates = new ArrayList<>();
        Class<?> current = type;
        while (current != null && !Object.class.equals(current)) {
            for (final Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!valueType.isAssignableFrom(field.getType())) {
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

    private static Optional<Method> selectPreferredMethod(final List<Method> candidates,
                                                          final List<String> preferredNames) {
        for (final String preferredName : preferredNames) {
            final List<Method> matches = new ArrayList<>();
            for (final Method candidate : candidates) {
                if (candidate.getName().equals(preferredName)) {
                    matches.add(candidate);
                }
            }
            if (matches.size() == 1) {
                return Optional.of(matches.get(0));
            }
            if (matches.size() > 1) {
                throw new IllegalStateException("Multiple preferred methods named " + preferredName +
                        ": " + describeMethods(matches));
            }
        }
        return Optional.empty();
    }

    private static Optional<Field> selectPreferredField(final List<Field> candidates,
                                                        final List<String> preferredNames) {
        for (final String preferredName : preferredNames) {
            final List<Field> matches = new ArrayList<>();
            for (final Field candidate : candidates) {
                if (candidate.getName().equals(preferredName)) {
                    matches.add(candidate);
                }
            }
            if (matches.size() == 1) {
                return Optional.of(matches.get(0));
            }
            if (matches.size() > 1) {
                throw new IllegalStateException("Multiple preferred fields named " + preferredName +
                        ": " + describeFields(matches));
            }
        }
        return Optional.empty();
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
