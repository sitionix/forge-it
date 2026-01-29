package com.sitionix.forgeit.mockmvc.internal.validator;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import org.skyscreamer.jsonassert.JSONAssert;

@UtilityClass
public class JsonComparator {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

    public static void compareJson(final String expectedJson, final String actualJson, final String... fieldsForIgnore)
            throws Exception {
        compareJson(expectedJson, actualJson, true, fieldsForIgnore);
    }

    public static void compareJson(final String expectedJson,
                                   final String actualJson,
                                   final boolean strict,
                                   final String... fieldsForIgnore) throws Exception {
        final JsonNode expected = MAPPER.readTree(expectedJson);
        final JsonNode actual = MAPPER.readTree(actualJson);
        removeFields(expected, fieldsForIgnore);
        removeFields(actual, fieldsForIgnore);
        JSONAssert.assertEquals(MAPPER.writeValueAsString(expected), MAPPER.writeValueAsString(actual), strict);
    }

    private static void removeFields(final JsonNode node, final String... fields) {
        if (node.isObject()) {
            for (final String f : fields) ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove(f);
            node.fields().forEachRemaining(e -> removeFields(e.getValue(), fields));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> removeFields(child, fields));
        }
    }
}
