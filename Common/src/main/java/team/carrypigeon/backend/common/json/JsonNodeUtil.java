package team.carrypigeon.backend.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JsonNode工具类，用于方便生成简单的JsonNode而不是通过ObjectMapper
 * */
public class JsonNodeUtil {

    public static JsonNode createJsonNode(String... keyValuePairs){
        if (keyValuePairs.length % 2 != 0) return null;
        ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            jsonNode.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return jsonNode; // Removed erroneous semicolon
    }
}
