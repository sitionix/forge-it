package com.sitionix.forgeit.application.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.experimental.UtilityClass;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@UtilityClass
public class EntityJsonComparator {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
            .configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    public static void assertMatchesJson(final Object actual,
                                         final String expectedJson,
                                         final Set<String> fieldsToIgnore) {
        assertJson(actual, expectedJson, fieldsToIgnore, false);
    }

    public static void assertMatchesJsonStrict(final Object actual,
                                               final String expectedJson,
                                               final Set<String> fieldsToIgnore) {
        assertJson(actual, expectedJson, fieldsToIgnore, true);
    }

    private static void assertJson(final Object actual,
                                   final String expectedJson,
                                   final Set<String> fieldsToIgnore,
                                   final boolean strict) {
        if (actual == null) {
            throw new AssertionError("Actual entity is null");
        }
        if (expectedJson == null) {
            throw new IllegalArgumentException("Expected json must not be null");
        }

        final JsonNode expectedNode;
        try {
            expectedNode = MAPPER.readTree(expectedJson);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to parse json", e);
        }

        final Set<Object> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        compareNode(expectedNode, actual, fieldsToIgnore, "$", strict, visited);
    }

    private static void compareNode(final JsonNode expected,
                                    final Object actual,
                                    final Set<String> fieldsToIgnore,
                                    final String path,
                                    final boolean strict,
                                    final Set<Object> visited) {
        if (expected.isObject()) {
            assertObjectNode(expected, actual, fieldsToIgnore, path, strict, visited);
            return;
        }

        if (expected.isArray()) {
            assertArrayNode(expected, actual, fieldsToIgnore, path, strict, visited);
            return;
        }

        if (expected.isNull()) {
            if (actual != null) {
                throw new AssertionError(String.format("Expected null at %s but was %s", path, actual));
            }
            return;
        }

        final JsonNode actualNode = toScalarNode(actual);
        boolean matches = expected.equals(actualNode);
        if (expected.isNumber() && actualNode.isNumber()) {
            matches = expected.decimalValue().compareTo(actualNode.decimalValue()) == 0;
        }
        if (!matches) {
            throw new AssertionError(String.format("Mismatch at %s: expected %s but was %s",
                    path,
                    expected,
                    actualNode));
        }
    }

    private static void assertObjectNode(final JsonNode expected,
                                         final Object actual,
                                         final Set<String> fieldsToIgnore,
                                         final String path,
                                         final boolean strict,
                                         final Set<Object> visited) {
        if (actual == null) {
            throw new AssertionError(String.format("Expected object at %s but was null", path));
        }

        if (markVisited(actual, visited)) {
            return;
        }

        final BeanWrapper wrapper = new BeanWrapperImpl(actual);
        final Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final String fieldName = entry.getKey();
            if (fieldsToIgnore != null && fieldsToIgnore.contains(fieldName)) {
                continue;
            }

            final Object actualValue;
            try {
                actualValue = wrapper.getPropertyValue(fieldName);
            } catch (final Exception e) {
                throw new AssertionError(String.format("Failed to read field '%s' at %s", fieldName, path), e);
            }

            compareNode(entry.getValue(),
                    actualValue,
                    fieldsToIgnore,
                    path + "." + fieldName,
                    strict,
                    visited);
        }

        if (strict) {
            assertNoUnexpectedFields(expected, wrapper, fieldsToIgnore, path, visited);
        }
    }

    private static void assertArrayNode(final JsonNode expected,
                                        final Object actual,
                                        final Set<String> fieldsToIgnore,
                                        final String path,
                                        final boolean strict,
                                        final Set<Object> visited) {
        if (actual == null) {
            throw new AssertionError(String.format("Expected array at %s but was null", path));
        }

        final List<Object> actualList = toList(actual, path);
        if (expected.size() != actualList.size()) {
            throw new AssertionError(String.format("Array size mismatch at %s: expected %d but was %d",
                    path,
                    expected.size(),
                    actualList.size()));
        }

        for (int index = 0; index < expected.size(); index++) {
            compareNode(expected.get(index),
                    actualList.get(index),
                    fieldsToIgnore,
                    path + "[" + index + "]",
                    strict,
                    visited);
        }
    }

    private static void assertNoUnexpectedFields(final JsonNode expected,
                                                 final BeanWrapper wrapper,
                                                 final Set<String> fieldsToIgnore,
                                                 final String path,
                                                 final Set<Object> visited) {
        final Iterator<String> fieldNames = wrapper.getPropertyDescriptors() != null
                ? Arrays.stream(wrapper.getPropertyDescriptors())
                .map(pd -> pd.getName())
                .filter(name -> !"class".equals(name))
                .iterator()
                : java.util.Collections.emptyIterator();

        final Set<String> expectedFields = new HashSet<>();
        expected.fieldNames().forEachRemaining(expectedFields::add);

        while (fieldNames.hasNext()) {
            final String fieldName = fieldNames.next();
            if (fieldsToIgnore != null && fieldsToIgnore.contains(fieldName)) {
                continue;
            }
            if (!expectedFields.contains(fieldName)) {
                final Object value = wrapper.getPropertyValue(fieldName);
                if (value != null && visited.contains(value)) {
                    continue;
                }
                if (!isEmptyValue(value)) {
                    throw new AssertionError(String.format("Unexpected field at %s: %s=%s",
                            path,
                            fieldName,
                            value));
                }
            }
        }
    }

    private static boolean isEmptyValue(final Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Iterable) {
            return !((Iterable<?>) value).iterator().hasNext();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        if (value instanceof CharSequence) {
            return ((CharSequence) value).length() == 0;
        }
        return Objects.equals(value, 0) || Objects.equals(value, 0L) || Objects.equals(value, 0.0);
    }

    private static boolean markVisited(final Object actual, final Set<Object> visited) {
        if (actual == null || isScalar(actual)) {
            return false;
        }
        if (visited.contains(actual)) {
            return true;
        }
        visited.add(actual);
        return false;
    }

    private static boolean isScalar(final Object actual) {
        return actual instanceof String
                || actual instanceof Number
                || actual instanceof Boolean
                || actual instanceof Enum<?>;
    }

    private static List<Object> toList(final Object actual, final String path) {
        if (actual.getClass().isArray()) {
            final int length = Array.getLength(actual);
            final List<Object> values = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                values.add(Array.get(actual, index));
            }
            return values;
        }

        if (actual instanceof Iterable) {
            final List<Object> values = new ArrayList<>();
            for (final Object item : (Iterable<?>) actual) {
                values.add(item);
            }
            return values;
        }

        throw new AssertionError(String.format("Expected array or collection at %s but was %s",
                path,
                actual.getClass().getName()));
    }

    private static JsonNode toScalarNode(final Object actual) {
        if (actual == null) {
            return NullNode.getInstance();
        }
        if (actual instanceof String) {
            return new TextNode((String) actual);
        }
        if (actual instanceof Boolean) {
            return BooleanNode.valueOf((Boolean) actual);
        }
        if (actual instanceof Integer || actual instanceof Long || actual instanceof Short || actual instanceof Byte) {
            return LongNode.valueOf(((Number) actual).longValue());
        }
        if (actual instanceof Float || actual instanceof Double) {
            return DoubleNode.valueOf(((Number) actual).doubleValue());
        }
        if (actual instanceof Enum<?>) {
            return new TextNode(((Enum<?>) actual).name());
        }
        return new TextNode(actual.toString());
    }
}
