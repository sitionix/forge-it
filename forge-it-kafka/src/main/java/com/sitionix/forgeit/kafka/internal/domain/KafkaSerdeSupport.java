package com.sitionix.forgeit.kafka.internal.domain;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class KafkaSerdeSupport {

    private KafkaSerdeSupport() {
    }

    @SuppressWarnings("rawtypes")
    static Serializer createSerializer(final Class<? extends Serializer> serializerClass,
                                       final Class<?> recordType) {
        if (serializerClass == null) {
            return null;
        }
        return (Serializer) createSerde(serializerClass, recordType, "serializer");
    }

    @SuppressWarnings("rawtypes")
    static Deserializer createDeserializer(final Class<? extends Deserializer> deserializerClass,
                                           final Class<?> recordType) {
        if (deserializerClass == null) {
            return null;
        }
        return (Deserializer) createSerde(deserializerClass, recordType, "deserializer");
    }

    private static Object createSerde(final Class<?> serdeClass,
                                      final Class<?> recordType,
                                      final String label) {
        final Constructor<?> noArgs = findNoArgsConstructor(serdeClass);
        if (noArgs != null) {
            return instantiate(noArgs, serdeClass, label);
        }
        final Object schema = resolveSchema(recordType);
        if (schema != null) {
            final Constructor<?> schemaConstructor = findSchemaConstructor(serdeClass, schema);
            if (schemaConstructor != null) {
                return instantiate(schemaConstructor, serdeClass, label, schema);
            }
        }
        throw new IllegalStateException("Kafka payload " + label + " " + serdeClass.getName()
                + " must declare a no-args or schema constructor");
    }

    private static Constructor<?> findNoArgsConstructor(final Class<?> serdeClass) {
        try {
            final Constructor<?> constructor = serdeClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (final NoSuchMethodException ex) {
            return null;
        }
    }

    private static Constructor<?> findSchemaConstructor(final Class<?> serdeClass, final Object schema) {
        for (final Constructor<?> constructor : serdeClass.getDeclaredConstructors()) {
            if (constructor.getParameterCount() != 1) {
                continue;
            }
            final Class<?> paramType = constructor.getParameterTypes()[0];
            if (paramType.isAssignableFrom(schema.getClass())) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        return null;
    }

    private static Object instantiate(final Constructor<?> constructor,
                                      final Class<?> serdeClass,
                                      final String label,
                                      final Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to instantiate Kafka payload " + label + " "
                    + serdeClass.getName(), ex);
        }
    }

    private static Object resolveSchema(final Class<?> recordType) {
        if (recordType == null) {
            return null;
        }
        try {
            final Method method = recordType.getDeclaredMethod("getClassSchema");
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                method.setAccessible(true);
                return method.invoke(null);
            }
        } catch (final ReflectiveOperationException ignored) {
        }
        try {
            final Field field = recordType.getDeclaredField("SCHEMA$");
            if (Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                return field.get(null);
            }
        } catch (final ReflectiveOperationException ignored) {
        }
        return null;
    }
}
