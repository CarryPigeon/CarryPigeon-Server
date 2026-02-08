package team.carrypigeon.backend.chat.domain.service.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleJsonSchemaValidatorTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validate_localRef_shouldValidateAgainstResolvedSchema() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "type": "object",
                  "required": ["a"],
                  "properties": {
                    "a": { "$ref": "#/$defs/A" }
                  },
                  "$defs": {
                    "A": {
                      "type": "object",
                      "required": ["x"],
                      "additionalProperties": false,
                      "properties": {
                        "x": { "type": "string", "minLength": 1 }
                      }
                    }
                  }
                }
                """);
        SimpleJsonSchemaValidator v = new SimpleJsonSchemaValidator();

        JsonNode ok = objectMapper.readTree("{\"a\":{\"x\":\"hi\"}}");
        assertTrue(v.validate(schema, ok).isEmpty());

        JsonNode bad = objectMapper.readTree("{\"a\":{}}");
        assertFalse(v.validate(schema, bad).isEmpty());
    }

    @Test
    void validate_unsupportedRemoteRef_shouldFail() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "type": "object",
                  "properties": {
                    "a": { "$ref": "http://example.com/schema.json" }
                  }
                }
                """);
        SimpleJsonSchemaValidator v = new SimpleJsonSchemaValidator();
        JsonNode data = objectMapper.readTree("{\"a\":1}");
        assertFalse(v.validate(schema, data).isEmpty());
    }

    @Test
    void validate_oneOf_shouldRequireExactlyOneMatch() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "oneOf": [
                    { "type": "string", "minLength": 1 },
                    { "type": "integer", "minimum": 1 }
                  ]
                }
                """);
        SimpleJsonSchemaValidator v = new SimpleJsonSchemaValidator();
        assertTrue(v.validate(schema, objectMapper.readTree("\"ok\"")).isEmpty());
        assertTrue(v.validate(schema, objectMapper.readTree("2")).isEmpty());
        assertFalse(v.validate(schema, objectMapper.readTree("0")).isEmpty());
    }

    @Test
    void validate_anyOf_shouldAllowAnyMatch() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "anyOf": [
                    { "type": "string", "minLength": 3 },
                    { "type": "string", "pattern": "^a.*" }
                  ]
                }
                """);
        SimpleJsonSchemaValidator v = new SimpleJsonSchemaValidator();
        assertTrue(v.validate(schema, objectMapper.readTree("\"abc\"")).isEmpty());
        assertTrue(v.validate(schema, objectMapper.readTree("\"a\"")).isEmpty());
        assertFalse(v.validate(schema, objectMapper.readTree("\"b\"")).isEmpty());
    }

    @Test
    void validate_allOf_shouldRequireAllMatch() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "allOf": [
                    { "type": "string", "minLength": 2 },
                    { "type": "string", "pattern": "^a.*" }
                  ]
                }
                """);
        SimpleJsonSchemaValidator v = new SimpleJsonSchemaValidator();
        assertTrue(v.validate(schema, objectMapper.readTree("\"ab\"")).isEmpty());
        assertFalse(v.validate(schema, objectMapper.readTree("\"a\"")).isEmpty());
        assertFalse(v.validate(schema, objectMapper.readTree("\"bb\"")).isEmpty());
    }

    @Test
    void validate_typeUnion_shouldSelectMatchingType() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "type": ["string", "null"],
                  "maxLength": 3
                }
                """);
        SimpleJsonSchemaValidator v = new SimpleJsonSchemaValidator();
        assertTrue(v.validate(schema, objectMapper.readTree("null")).isEmpty());
        assertTrue(v.validate(schema, objectMapper.readTree("\"abc\"")).isEmpty());
        assertFalse(v.validate(schema, objectMapper.readTree("\"abcd\"")).isEmpty());
    }
}
