package com.sitionix.forgeit.application.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class EntityJsonComparator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void assertMatchesJson(final Object actual,
                                         final String expectedJson,
                                         final Set<String> fieldsToIgnore) {
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
            throw new IllegalStateException("Failed to parse expected json", e);
        }

        compareNode(expectedNode, actual, fieldsToIgnore, "$");
    }

    public static void assertMatchesJsonStrict(final Object actual,
                                               final String expectedJson,
                                               final Set<String> fieldsToIgnore) {
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
            throw new IllegalStateException("Failed to parse expected json", e);
        }

        final JsonNode actualNode = MAPPER.valueToTree(actual);
        removeFields(expectedNode, fieldsToIgnore);
        removeFields(actualNode, fieldsToIgnore);
        if (!expectedNode.equals(actualNode)) {
            throw new AssertionError(String.format("Strict match failed: expected %s but was %s",
                    expectedNode,
                    actualNode));
        }
    }

    private static void compareNode(final JsonNode expected,
                                    final Object actual,
                                    final Set<String> fieldsToIgnore,
                                    final String path) {
        if (expected.isObject()) {
            assertObjectNode(expected, actual, fieldsToIgnore, path);
            return;
        }

        if (expected.isArray()) {
            assertArrayNode(expected, actual, fieldsToIgnore, path);
            return;
        }

        if (expected.isNull()) {
            if (actual != null) {
                throw new AssertionError(String.format("Expected null at %s but was %s", path, actual));
            }
            return;
        }

        final JsonNode actualNode = MAPPER.valueToTree(actual);
        if (!expected.equals(actualNode)) {
            throw new AssertionError(String.format("Mismatch at %s: expected %s but was %s",
                    path,
                    expected,
                    actualNode));
        }
    }

    private static void assertObjectNode(final JsonNode expected,
                                         final Object actual,
                                         final Set<String> fieldsToIgnore,
                                         final String path) {
        if (actual == null) {
            throw new AssertionError(String.format("Expected object at %s but was null", path));
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

            compareNode(entry.getValue(), actualValue, fieldsToIgnore, path + "." + fieldName);
        }
    }

    private static void assertArrayNode(final JsonNode expected,
                                        final Object actual,
                                        final Set<String> fieldsToIgnore,
                                        final String path) {
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
            compareNode(expected.get(index), actualList.get(index), fieldsToIgnore, path + "[" + index + "]");
        }
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

    private static void removeFields(final JsonNode node, final Set<String> fieldsToIgnore) {
        if (fieldsToIgnore == null || fieldsToIgnore.isEmpty()) {
            return;
        }
        if (node.isObject()) {
            for (final String field : fieldsToIgnore) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove(field);
            }
            node.fields().forEachRemaining(e -> removeFields(e.getValue(), fieldsToIgnore));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> removeFields(child, fieldsToIgnore));
        }
    }
}
