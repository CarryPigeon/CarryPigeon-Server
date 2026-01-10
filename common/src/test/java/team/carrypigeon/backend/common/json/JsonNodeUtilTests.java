package team.carrypigeon.backend.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonNodeUtilTests {

    @Test
    void createJsonNode_oddPairs_returnsNull() {
        new JsonNodeUtil();
        assertNull(JsonNodeUtil.createJsonNode("k1", "v1", "k2"));
    }

    @Test
    void createJsonNode_evenPairs_createsObjectNode() {
        JsonNode node = JsonNodeUtil.createJsonNode("k1", "v1", "k2", "v2");
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("v1", node.get("k1").asText());
        assertEquals("v2", node.get("k2").asText());
    }

    @Test
    void getValue_textual_returnsString() {
        assertEquals("hello", JsonNodeUtil.getValue(JsonNodeFactory.instance.textNode("hello")));
    }

    @Test
    void getValue_long_returnsLong() {
        assertEquals(123L, JsonNodeUtil.getValue(JsonNodeFactory.instance.numberNode(123L)));
    }

    @Test
    void getValue_intAndOtherNumbers_returnInt() {
        assertEquals(42, JsonNodeUtil.getValue(JsonNodeFactory.instance.numberNode(42)));
        assertEquals(3, JsonNodeUtil.getValue(JsonNodeFactory.instance.numberNode(3.14)));
    }

    @Test
    void getValue_boolean_returnsBoolean() {
        assertEquals(true, JsonNodeUtil.getValue(JsonNodeFactory.instance.booleanNode(true)));
    }

    @Test
    void getValue_array_returnsNodeItself() {
        JsonNode array = JsonNodeFactory.instance.arrayNode().add("a");
        Object value = JsonNodeUtil.getValue(array);
        assertSame(array, value);
    }

    @Test
    void getValue_objectOrOther_returnsNull() {
        ObjectNode obj = JsonNodeFactory.instance.objectNode().put("k", "v");
        assertNull(JsonNodeUtil.getValue(obj));
    }
}
